package io.github.colinzhu.routeswitcher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class RouteSwitcherReverseVerticle extends AbstractVerticle {
    private HttpProxy httpProxy;
    private HttpProxy httpsProxy;
    private Router defaultRequestHandler;

    private final RuleManager ruleManager = new RuleManageImpl();

    @Override
    public void start() {
        int port = config().getInteger("port");
        ruleManager.loadRules();
        httpProxy = prepareHttpProxy(false);
        httpsProxy = prepareHttpProxy(true);
        defaultRequestHandler = prepareDefaultRequestHandler();

        vertx.createHttpServer(new HttpServerOptions().setSsl(false).setPort(port))
                .requestHandler(this::handleRequest)
                .listen(port)
                .onSuccess(res -> log.info("proxyHandler server started at port: {}", port))
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
        String uri = request.uri();
        var firstMatchedRule = ruleManager.getRules().stream().filter(entry -> uri.startsWith("/" + entry.getUriPrefix())).findFirst();

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
        httpProxy.originSelector(this::selectOrigin);
        return httpProxy;
    }

    private Future<SocketAddress> selectOrigin(HttpServerRequest request) {
        String uri = request.uri();
        return ruleManager.getRules().stream().filter(entry -> uri.startsWith("/" + entry.getUriPrefix())).findFirst()
                .map(entry -> {
                    log.info("{} ==> {}", request.absoluteURI(), entry.getTarget() + uri);
                    return Future.succeededFuture(getTargetSocketAddress(entry.getTarget()));
                })
                .orElse(Future.failedFuture("No matching rule found for URI: " + uri));
    }

    private SocketAddress getTargetSocketAddress(String targetServer) {
        URI uri = URI.create(targetServer);
        int port = uri.getPort() != -1 ? uri.getPort() : uri.getScheme().equals("https") ? 443 : 80;
        return SocketAddress.inetSocketAddress(port, uri.getHost());
    }
}
