package io.github.colinzhu.routeswitcher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Optional;

@Slf4j
public class RouteSwitcherReverseVerticle extends AbstractVerticle {
    private HttpProxy httpProxy;
    private HttpProxy httpsProxy;
    private Router defaultRequestHandler;

    private final RuleManager ruleManager = new RuleManageImpl();

    @Override
    public void start() {
        int port = config().getInteger("port");
        httpProxy = prepareHttpProxy(false);
        httpsProxy = prepareHttpProxy(true);
        defaultRequestHandler = prepareDefaultRequestHandler();

        vertx.createHttpServer(new HttpServerOptions().setSsl(false).setPort(port))
                .webSocketHandler(new WebSocketHandler(vertx))
                .requestHandler(this::handleRequest)
                .listen(port)
                .onSuccess(res -> log.info("reverse proxy server started at port: {}", res.actualPort()))
                .onFailure(err -> log.error("error start proxyHandler server", err));
    }

    private Router prepareDefaultRequestHandler() {
        defaultRequestHandler = Router.router(vertx);
        defaultRequestHandler.route().handler(StaticHandler.create("web"));
        defaultRequestHandler.route().handler(BodyHandler.create());
        defaultRequestHandler.route("/rule-manage/*").subRouter(new RuleManageHandler(vertx, ruleManager).getHandler());
        return defaultRequestHandler;
    }

    private void handleRequest(HttpServerRequest request) {
        var firstMatchedRule = getFirstMatchedRule(request);
        if (firstMatchedRule.isEmpty()) {
            defaultRequestHandler.handle(request);
            return;
        }

        String targetServer = firstMatchedRule.get().getTarget();
        if (targetServer.startsWith("https")) {
            httpsProxy.handle(request);
        } else {
            httpProxy.handle(request);
        }
    }

    private HttpProxy prepareHttpProxy(boolean isSsl) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(isSsl));
        HttpProxy httpProxy = HttpProxy.reverseProxy(httpClient);
        httpProxy.originRequestProvider(this::prepareTargetRequest);
        return httpProxy;
    }

    private Future<HttpClientRequest> prepareTargetRequest(HttpServerRequest serverRequest, HttpClient client) {
        var firstMatchedRule = getFirstMatchedRule(serverRequest);
        String targetServer = firstMatchedRule.get().getTarget();

        return client
                .request(new RequestOptions().setServer(getTargetSocketAddress(targetServer)))
                .onSuccess(clientRequest -> clientRequest.response()
                        .onSuccess(response -> log.info("{} ===> {} ({})", serverRequest.absoluteURI(), targetServer + serverRequest.uri(), response.statusCode()))
                        .onFailure(err -> log.error("error response from target server", err)))
                .onFailure(err -> log.error("{} ===> {} (error)", serverRequest.absoluteURI(), targetServer + serverRequest.uri(), err));
    }

    private Optional<Rule> getFirstMatchedRule(HttpServerRequest serverRequest) {
        String uri = serverRequest.uri();
        return ruleManager.getRules().stream().filter(entry -> uri.startsWith(entry.getUriPrefix())).findFirst();
    }

    private SocketAddress getTargetSocketAddress(String targetServer) {
        URI uri = URI.create(targetServer);
        int port = uri.getPort() != -1 ? uri.getPort() : uri.getScheme().equals("https") ? 443 : 80;
        return SocketAddress.inetSocketAddress(port, uri.getHost());
    }
}
