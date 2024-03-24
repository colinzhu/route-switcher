package io.github.colinzhu.routeswitcher;

import java.util.Set;

public interface RuleManager {
    Set<Rule> getRules();
    void loadRules();
    void persistRules();
    void addOrUpdate(Rule rule);
    void deleteRule(Rule rule);

}
