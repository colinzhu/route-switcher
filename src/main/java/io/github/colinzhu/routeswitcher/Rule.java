package io.github.colinzhu.routeswitcher;

import lombok.Data;

import java.util.Objects;

@Data
public class Rule {
    private String uriPrefix;
    private String target;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(uriPrefix, rule.uriPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uriPrefix);
    }
}
