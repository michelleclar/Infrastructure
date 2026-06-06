package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Configuration for {@link TaskGroupHandler}.
 *
 * @param join join rule (defaults to {@link JoinRule#ALL} when {@code null}).
 * @param tasks the child task definitions; nullable list is treated as empty.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskGroupConfig(JoinRule join, List<TaskGroupChild> tasks) {

    /** Aggregation strategy applied to child outcomes. */
    public enum JoinRule {
        ALL("all"),
        ANY("any");

        private final String wireName;

        JoinRule(String wireName) {
            this.wireName = wireName;
        }

        @JsonValue
        public String wireName() {
            return wireName;
        }

        @JsonCreator
        public static JoinRule fromWire(String value) {
            if (value == null) {
                return null;
            }
            String lc = value.trim().toLowerCase();
            for (JoinRule r : values()) {
                if (r.wireName.equals(lc)) {
                    return r;
                }
            }
            throw new IllegalArgumentException("Unknown join rule: " + value);
        }
    }

    /**
     * A child task definition embedded inside a task group.
     *
     * @param id stable identifier; combined with the parent node id to look up child results.
     * @param label optional UI label.
     * @param type node type discriminator (e.g. {@code approvalTask}).
     * @param config raw configuration node interpreted by the child handler.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TaskGroupChild(String id, String label, String type, JsonNode config) {}
}
