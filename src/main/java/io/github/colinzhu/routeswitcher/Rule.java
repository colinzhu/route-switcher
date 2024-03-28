package io.github.colinzhu.routeswitcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Objects;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Rule {
    private String uriPrefix;
    private String fromIP;
    private String target;
    private String updateBy;
    private Long updateTime;
    private String remark;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(uriPrefix, rule.uriPrefix) && Objects.equals(fromIP, rule.fromIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uriPrefix + fromIP);
    }
}
