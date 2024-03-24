package io.github.colinzhu.routeswitcher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class RouteSwitcherForwardVerticle extends AbstractVerticle {
    private HttpProxy httpProxy;
    private HttpProxy httpsProxy;

    private final Map<String, String> rules = Map.of(
            "local", "http://localhost:7070"
    );
    @Override
    public void start() {
        int port = config().getInteger("port");
        httpProxy = getHttpProxy(false);
        httpsProxy = getHttpProxy(true);


        vertx.createHttpServer(new HttpServerOptions()
                        .setSsl(false)
                        .setPort(port)).requestHandler(this::handle2).listen(port)
                .onSuccess(res -> log.info("proxyHandler server started at port: {}", port))
                .onFailure(err -> log.error("error start proxyHandler server", err));
    }

    // reject invalid target name with 404
    private void handleInboundRequest(HttpServerRequest request) {
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

    private void handle2(HttpServerRequest request) {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
        String targetName = request.uri().split("/")[1];
        String targetServer = rules.get(targetName);


        if (targetServer == null) {
            request.response().end();
            return;
        }

        String targetUri = getTargetUri(request.uri(), targetName);
        String targetAbsoluteUri = targetServer + targetUri;

        client
                .request(new RequestOptions().setServer(getTargetSockAddress(targetServer)).setURI(targetUri).setAbsoluteURI(targetAbsoluteUri))
                .onSuccess(targetReq -> log.info("{} ===> {}", request.absoluteURI(), targetReq.absoluteURI()))
                .compose(httpClientRequest -> httpClientRequest.send())
                .compose(httpClientResponse -> request.response().setStatusCode(httpClientResponse.statusCode()).end("test"))
                .onSuccess(res -> log.info("request handled"))
                .onFailure(err -> {
                    log.error("error handle request", err);
                    request.response().setStatusCode(500).end();
                });
    }

    private HttpProxy getHttpProxy(boolean isSsl) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(isSsl));
        HttpProxy httpProxy = HttpProxy.reverseProxy(httpClient);
        httpProxy.originRequestProvider(this::prepareTargetRequest);
        return httpProxy;
    }

    private Future<HttpClientRequest> prepareTargetRequest(HttpServerRequest incomingReq, HttpClient client) {
        String targetName = incomingReq.uri().split("/")[1];
        String targetServer = rules.get(targetName);
        String targetUri = getTargetUri(incomingReq.uri(), targetName);
        String targetAbsoluteUri = targetServer + targetUri;

        return client
                .request(new RequestOptions().setServer(getTargetSockAddress(targetServer)).setURI(targetUri).setAbsoluteURI(targetAbsoluteUri))
                .onSuccess(targetReq -> log.info("{} ===> {}", incomingReq.absoluteURI(), targetReq.absoluteURI()));
    }
    private String getTargetUri(String oriUri, String targetName) {
        return oriUri.substring(("/" + targetName).length());
    }

    private SocketAddress getTargetSockAddress(String targetServer) {
        // if targetServer (http://localhost:7070 or https://zephy.tech) contains port, use the port, else if it's https use 443, else use 80
        String hostAndPort = targetServer.split("://")[1];
        int targetPort = hostAndPort.contains(":") ? Integer.parseInt(hostAndPort.split(":")[1]) : hostAndPort.startsWith("https") ? 443 : 80;


        String targetHost = targetServer.split("://")[1].split(":")[0];
        //log.info("target server:{}, target host:{}, target port:{}", targetServer, targetHost, targetPort);
        return SocketAddress.inetSocketAddress(targetPort, targetHost);
    }

    // target name -> target server (host, port)
}
