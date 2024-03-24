package io.github.colinzhu.routeswitcher;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
class RuleManageHandler {
    private final Vertx vertx;
    private final RuleManager ruleManager;

    public Router getHandler() {
        Router router = Router.router(vertx);
        router.route(HttpMethod.GET, "/api/rules").handler(this::getRules);
        router.route(HttpMethod.POST, "/api/rules").handler(this::addOrUpdateOneRule);
        router.route(HttpMethod.DELETE, "/api/rules").handler(this::deleteOneRule);
        return router;
    }

    private void getRules(RoutingContext routingContext) {
        log.info("get rules request body:{}", routingContext.body().asString());
        Future.succeededFuture()
                .onSuccess(res -> routingContext.json(ruleManager.getRules()))
                .onFailure(err -> routingContext.response().setStatusCode(500).end(Json.encode(Map.of("reason", "error"))));
    }

    private void addOrUpdateOneRule(RoutingContext routingContext) {
        log.info("update one rule request body:{}", routingContext.body().asString());

        try {
            Rule rule = routingContext.body().asPojo(Rule.class);
            ruleManager.addOrUpdate(rule);
            routingContext.json(ruleManager.getRules());
        } catch (Exception e) {
            routingContext.response().setStatusCode(500).end(Json.encode(Map.of("reason", e.getMessage())));
        }
    }

    private void deleteOneRule(RoutingContext routingContext) {
        log.info("delete one rule request body:{}", routingContext.body().asString());

        try {
            Rule rule = routingContext.body().asPojo(Rule.class);
            ruleManager.delete(rule);
            routingContext.json(ruleManager.getRules());
        } catch (Exception e) {
            routingContext.response().setStatusCode(500).end(Json.encode(Map.of("reason", e.getMessage())));
        }
    }

}
