package io.github.colinzhu.routeswitcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class RuleManageImpl implements RuleManager {
    private Map<String, String> rules = new HashMap<>();
    //private JsonArray rules = new JsonArray();

    @Override
    public void loadRules() {
        try {
            String rulesStr = Files.readString(Path.of("rules.json"));
            JsonArray array = new JsonArray(rulesStr);
            array.forEach(item -> {
                JsonObject rule = (JsonObject) item;
                rules.put(rule.getString("uriPrefix"), rule.getString("target"));
            });
        } catch (IOException e) {
            log.warn("no rules.json file found, use empty rules", e);
        }
    }

    @Override
    public void persistRules() {
        JsonArray collect = rules.entrySet().stream().map(entry -> new JsonObject().put("uriPrefix", entry.getKey()).put("target", entry.getValue()))
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        try {
            Files.writeString(Path.of("rules.json"), collect.encodePrettily());
        } catch (IOException e) {
            log.error("fail to save rules to file", e);
            throw new RuntimeException(e);
        }
    }
}
