package io.github.colinzhu.routeswitcher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;

@Slf4j
public class RouteSwitcherReverseVerticle extends AbstractVerticle {
    private HttpProxy httpProxy;
    private HttpProxy httpsProxy;

    private final Map<String, String> rules = Map.of(
            "local", "http://localhost:7070",
            "zephy", "https://zephy.tech"
    );

    @Override
    public void start() {
        int port = config().getInteger("port");
        httpProxy = getHttpProxy(false);
        httpsProxy = getHttpProxy(true);


        vertx.createHttpServer(new HttpServerOptions().setSsl(false).setPort(port))
                .requestHandler(this::handleRequest)
                .listen(port)
                .onSuccess(res -> log.info("proxyHandler server started at port: {}", port))
                .onFailure(err -> log.error("error start proxyHandler server", err));
    }

    // reject invalid target name with 404
    private void handleRequest(HttpServerRequest request) {
        String targetName = request.uri().split("/")[1];
        String targetServer = rules.get(targetName);

        if (targetServer == null) {
            request.response().end();
            return;
        }

        //log.info("target name:{}, target server:{}", targetName, targetServer);

        if (targetServer.startsWith("https")) {
            httpsProxy.handle(request);
        } else {
            httpProxy.handle(request);
        }
    }

    private HttpProxy getHttpProxy(boolean isSsl) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(isSsl));
        HttpProxy httpProxy = HttpProxy.reverseProxy(httpClient);
        httpProxy.originSelector(this::selectOrigin);
        //httpProxy.originRequestProvider(this::prepareTargetRequest);
        return httpProxy;
    }

    private Future<SocketAddress> selectOrigin(HttpServerRequest request) {
        String uri = request.uri();
        return rules.entrySet().stream()
                .filter(entry -> uri.startsWith("/" + entry.getKey()))
                .findFirst()
                .map(entry -> Future.succeededFuture(getTargetSocketAddress(entry.getValue())))
                .orElse(Future.failedFuture("No matching rule found for URI: " + uri));
    }

    private Future<HttpClientRequest> prepareTargetRequest(HttpServerRequest request, HttpClient client) {
        String targetName = request.uri().split("/")[1];
        String targetServer = rules.get(targetName);
        String targetUri = getTargetUri(request.uri(), targetName);
        String targetAbsoluteUri = targetServer + targetUri;

        return client
                .request(new RequestOptions().setServer(getTargetSocketAddress(targetServer)).setURI(targetUri).setAbsoluteURI(targetAbsoluteUri))
                .onSuccess(targetReq -> log.info("{} ===> {}", request.absoluteURI(), targetReq.absoluteURI()));
    }

    private String getTargetUri(String oriUri, String targetName) {
        return oriUri.substring(("/" + targetName).length());
    }

    private SocketAddress getTargetSocketAddress(String targetServer) {
        URI uri = URI.create(targetServer);
        int port = uri.getPort() != -1 ? uri.getPort() : uri.getScheme().equals("https") ? 443 : 80;
        return SocketAddress.inetSocketAddress(port, uri.getHost());
    }
}
