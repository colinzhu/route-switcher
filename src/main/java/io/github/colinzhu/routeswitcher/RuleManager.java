package io.github.colinzhu.routeswitcher;

import java.util.Set;

interface RuleManager {
    Set<Rule> getRules();
    void addOrUpdate(Rule rule);
    void delete(Rule rule);

}
