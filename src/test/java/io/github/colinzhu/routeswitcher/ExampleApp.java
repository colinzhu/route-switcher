package io.github.colinzhu.routeswitcher;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExampleApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(TestHttpServerVerticle.class, new DeploymentOptions().setConfig(new JsonObject().put("port", 7070)));
        vertx.deployVerticle(RouteSwitcherReverseVerticle.class, new DeploymentOptions().setConfig(new JsonObject().put("port", 8080)));
    }
}