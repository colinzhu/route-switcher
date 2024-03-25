package io.github.colinzhu.routeswitcher;

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
        log.debug("get rules request body:{}", routingContext.body().asString());
        routingContext.json(ruleManager.getRules());
    }

    private void addOrUpdateOneRule(RoutingContext routingContext) {
        log.debug("update one rule request body:{}", routingContext.body().asString());
        try {
            Rule rule = routingContext.body().asPojo(Rule.class);
            ruleManager.addOrUpdate(rule).onSuccess(ignored -> routingContext.json(ruleManager.getRules()));
        } catch (Exception e) {
            routingContext.response().setStatusCode(500).end(Json.encode(Map.of("reason", e.getMessage())));
        }
    }

    private void deleteOneRule(RoutingContext routingContext) {
        log.debug("delete one rule request body:{}", routingContext.body().asString());
        try {
            Rule rule = routingContext.body().asPojo(Rule.class);
            ruleManager.delete(rule).onSuccess(ignored -> routingContext.json(ruleManager.getRules()));
        } catch (Exception e) {
            routingContext.response().setStatusCode(500).end(Json.encode(Map.of("reason", e.getMessage())));
        }
    }

}
