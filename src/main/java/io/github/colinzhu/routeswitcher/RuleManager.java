package io.github.colinzhu.routeswitcher;

import java.util.Map;

public interface RuleManager {
    Map<String, String> getRules();
    void loadRules();
    void persistRules();
}
