package io.github.colinzhu.routeswitcher;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        //createSwitcher(vertx, 8080);
        vertx.deployVerticle(TestHttpServerVerticle.class, new DeploymentOptions().setConfig(new JsonObject().put("port", 7070)));
        vertx.deployVerticle(RouteSwitcherReverseVerticle.class, new DeploymentOptions().setConfig(new JsonObject().put("port", 8080)));
    }

    private static void createSwitcher(Vertx vertx, int port) {
        // copy the request to make a request to another target server


        // reject invalid target name with 404
        Handler<HttpServerRequest> inboundRqstHandler = req -> {
            log.info("request:{}", req.uri());
            getHttpProxy(vertx).handle(req);
        };

        vertx.createHttpServer(new HttpServerOptions()
                .setSsl(false)
                .setPort(port)).requestHandler(inboundRqstHandler).listen(port)
                .onSuccess(res -> log.info("proxyHandler server started at port: {}", port))
                .onFailure(err -> log.error("error start proxyHandler server", err));
    }

    private static HttpProxy getHttpProxy(Vertx vertx) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
        HttpProxy proxyHandler = HttpProxy.reverseProxy(httpClient);
        proxyHandler.originRequestProvider(
                (incomingReq, client) -> {
                    SocketAddress targetServer = getTargetServer(incomingReq);
                    String targetUri = getTargetUri(incomingReq.uri());
                    //log.info("{} ===> {}{}", incomingReq.absoluteURI(), targetServer, targetUri);
                    return client.request(new RequestOptions()
                            .setServer(targetServer)
                            .setURI(targetUri))
                            .onSuccess(targetReq -> {
                                log.info("{} ===> {}", incomingReq.absoluteURI(), targetReq.absoluteURI());
                            });
                }
        );
        return proxyHandler;
    }

    private static String getTargetUri(String oriUri) {
        String newUri = oriUri;
        if (oriUri.startsWith("/zephy")) {
            newUri = newUri.substring("/zephy".length());
        }
        //log.info("ori uri:{} ===> new uri:{}", oriUri, newUri);
        return newUri;
    }
    private static SocketAddress getTargetServer(HttpServerRequest request) {
        log.info("request:{}", request.uri());
        request.body().onSuccess(res -> log.info("request body:{}", res.toString()));
        return SocketAddress.inetSocketAddress(443, "zephy.tech");
    }

    // target name -> target server (host, port)
}