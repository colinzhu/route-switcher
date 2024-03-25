package io.github.colinzhu.routeswitcher;

import io.vertx.core.Future;

import java.util.Set;

interface RuleManager {
    Set<Rule> getRules();
    Future<Void> addOrUpdate(Rule rule);
    Future<Void> delete(Rule rule);

}
