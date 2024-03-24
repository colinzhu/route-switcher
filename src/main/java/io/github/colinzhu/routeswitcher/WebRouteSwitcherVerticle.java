package io.github.colinzhu.routeswitcher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class WebRouteSwitcherVerticle extends AbstractVerticle {
    private HttpProxy httpProxy;
    private HttpProxy httpsProxy;

    private Map<String, String> rules = Map.of(
            "local", "http://localhost:7070",
            "zephy", "https://zephy.tech"
    );
    @Override
    public void start() {
        int port = config().getInteger("port");
        httpProxy = getProxyHandler(false);
        httpsProxy = getProxyHandler(true);


        vertx.createHttpServer(new HttpServerOptions()
                        .setSsl(false)
                        .setPort(port)).requestHandler(this::handleInboundRequest).listen(port)
                .onSuccess(res -> log.info("proxyHandler server started at port: {}", port))
                .onFailure(err -> log.error("error start proxyHandler server", err));
    }

    private void handleInboundRequest(HttpServerRequest req) {
        log.info("inbound request:{}", req.uri());
        // reject invalid target name with 404
        String targetName = req.uri().split("/")[1];
        String targetServer = rules.get(targetName);
        log.info("target name:{}, target server:{}", targetName, targetServer);
        if (targetServer.startsWith("https")) {
            httpsProxy.handle(req);
        } else {
            httpProxy.handle(req);
        }
    }

    private HttpProxy getProxyHandler(boolean isSsl) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(isSsl));
        HttpProxy httpProxy = HttpProxy.reverseProxy(httpClient);
        httpProxy.originRequestProvider(this::prepareTargetRequest);
        return httpProxy;
    }

    private Future<HttpClientRequest> prepareTargetRequest(HttpServerRequest incomingReq, HttpClient client) {
        SocketAddress targetServer = getTargetServer(incomingReq);
        String targetUri = getTargetUri(incomingReq.uri());
        return client
                .request(new RequestOptions().setServer(targetServer).setURI(targetUri))
                .onSuccess(targetReq -> log.info("{} ===> {}", incomingReq.absoluteURI(), targetReq.absoluteURI()));
    }
    private static String getTargetUri(String oriUri) {
        String newUri = oriUri;
        String targetName = oriUri.split("/")[1];
        if (oriUri.startsWith("/" + targetName)) {
            newUri = newUri.substring(("/" + targetName).length());
        }
        log.info("ori uri:{} ===> new uri:{}", oriUri, newUri);
        return newUri;
    }

    private SocketAddress getTargetServer(HttpServerRequest request) {
        log.info("request:{}", request.uri());
        String targetName = request.uri().split("/")[1];
        String targetServer = rules.get(targetName);
        // if targetServer (http://localhost:7070 or https://zephy.tech) contains port, use the port, else if it's https use 443, else use 80
        String hostAndPort = targetServer.split("://")[1];
        int targetPort = hostAndPort.contains(":") ? Integer.parseInt(hostAndPort.split(":")[1]) : hostAndPort.startsWith("https") ? 443 : 80;


        String targetHost = targetServer.split("://")[1].split(":")[0];
        log.info("target name:{}, target server:{}, target host:{}", targetName, targetServer, targetHost);
        request.body().onSuccess(res -> log.info("request body:{}", res.toString()));
        return SocketAddress.inetSocketAddress(targetPort, targetHost);
    }

    // target name -> target server (host, port)
}
