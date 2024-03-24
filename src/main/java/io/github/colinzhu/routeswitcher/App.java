package io.github.colinzhu.routeswitcher;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class App {
    private static final String APP_CONFIG_FILE = "config.json";

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(RouteSwitcherReverseVerticle.class, new DeploymentOptions().setConfig(loadConfig()));
    }

    private static JsonObject loadConfig() {
        try {
            String rulesStr = Files.readString(Path.of(APP_CONFIG_FILE));
            return new JsonObject(rulesStr);
        } catch (IOException e) {
            log.warn("no config.json file found, use random port", e);
            return new JsonObject().put("port", 0);
        }
    }

}