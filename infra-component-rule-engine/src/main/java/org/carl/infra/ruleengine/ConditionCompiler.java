package org.carl.infra.ruleengine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/** Compiles application-bound JSON maps into parameterized QLExpress conditions. */
public final class ConditionCompiler {
    static final String PARAMETER_PREFIX = "__condition_";
    private static final int MAX_DEPTH = 32;
    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    private static final Set<String> OPERATORS = Set.of("==", "!=", ">", ">=", "<", "<=", "in");

    private final ExpressionExecutor executor;

    public ConditionCompiler() {
        this(ExpressionFactory.getDefault());
    }

    /** Uses the selected executor for the compiled script's syntax check. */
    public ConditionCompiler(ExpressionExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompiledCondition compile(Map<String, ?> tree) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String script = compileNode(Objects.requireNonNull(tree, "tree"), parameters, 1);
        executor.check(script);
        return new CompiledCondition(script, parameters);
    }

    private String compileNode(Map<?, ?> node, Map<String, Object> parameters, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("condition depth exceeds " + MAX_DEPTH);
        }
        if (node.keySet().equals(Set.of("combinator", "rules"))) {
            Object combinator = node.get("combinator");
            if (!"and".equals(combinator) && !"or".equals(combinator)) {
                throw new IllegalArgumentException("combinator must be and or or");
            }
            if (!(node.get("rules") instanceof List<?> rules) || rules.isEmpty()) {
                throw new IllegalArgumentException("rules must be a nonempty list");
            }
            StringJoiner group = new StringJoiner("and".equals(combinator) ? " && " : " || ", "(", ")");
            for (Object rule : rules) {
                if (!(rule instanceof Map<?, ?> child)) {
                    throw new IllegalArgumentException("rule must be a map");
                }
                group.add(compileNode(child, parameters, depth + 1));
            }
            return group.toString();
        }
        if (!node.keySet().equals(Set.of("field", "operator", "value"))) {
            throw new IllegalArgumentException("expected group or leaf with field, operator and value only");
        }
        if (!(node.get("field") instanceof String field) || !PATH.matcher(field).matches()) {
            throw new IllegalArgumentException("invalid field path");
        }
        String[] segments = field.split("\\.");
        if (segments[0].startsWith(PARAMETER_PREFIX)) {
            throw new IllegalArgumentException("field root uses reserved parameter namespace");
        }
        if (!(node.get("operator") instanceof String operator) || !OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("unsupported operator");
        }
        StringBuilder access = new StringBuilder(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            access.append("['").append(segments[i]).append("']");
        }
        String parameter = PARAMETER_PREFIX + parameters.size();
        parameters.put(parameter, node.get("value"));
        return "(" + access + " " + operator + " " + parameter + ")";
    }

    /** Parameter map is an immutable shallow snapshot; explicit null values are retained. */
    public record CompiledCondition(String script, Map<String, Object> parameters) {
        public CompiledCondition {
            Objects.requireNonNull(script, "script");
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
    }
}
