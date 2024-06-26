package io.github.colinzhu.routeswitcher;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Data
@Slf4j
class RuleManagerFileStoreImpl implements RuleManager {
    private Set<Rule> rules = new HashSet<>();
    private static final String RULES_FILE_NAME = "rules.json";

    public RuleManagerFileStoreImpl() {
        loadRules();
    }

    private void loadRules() {
        try {
            String rulesStr = Files.readString(Path.of(RULES_FILE_NAME));
            JsonArray array = new JsonArray(rulesStr);
            array.forEach(entry -> rules.add(((JsonObject)entry).mapTo(Rule.class)));
        } catch (IOException e) {
            log.warn("no rules.json file found, use empty rules", e);
        } catch (RuntimeException e) {
            log.error("fail to load rules from file", e);
        }
    }

    private void persistRules() {
        try {
            Files.writeString(Path.of(RULES_FILE_NAME), new JsonArray(rules.stream().toList()).encodePrettily());
        } catch (IOException e) {
            log.error("fail to save rules to file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<Void> addOrUpdate(Rule rule) {
        rules.remove(rule);
        rules.add(rule);
        persistRules();
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> delete(Rule rule) {
        rules.remove(rule);
        persistRules();
        return Future.succeededFuture();
    }
}
