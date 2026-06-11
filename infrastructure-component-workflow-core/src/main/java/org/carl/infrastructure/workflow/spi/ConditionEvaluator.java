package org.carl.infrastructure.workflow.spi;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;

import org.carl.infrastructure.workflow.definition.NodeResult;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Condition evaluator backed by Jakarta EL (JSR-341).
 *
 * <p>Public API kept identical to the previous self-built implementation:
 *
 * <ul>
 *   <li>{@link #evaluate(String, NodeExecutionContext)}: {@code null} / blank → {@code false}.
 *   <li>{@link #evaluateOrTrue(String, NodeExecutionContext)}: {@code null} / blank → {@code true}.
 * </ul>
 *
 * <h2>Expression grammar</h2>
 *
 * <p>Top-level {@code "true"} / {@code "false"} (case-insensitive) are accepted as plain string
 * literals. Everything else must be wrapped in {@code ${...}}; inside the wrapper, Jakarta EL
 * syntax is used.
 *
 * <p>Top-level variables exposed to the expression:
 *
 * <ul>
 *   <li>{@code variables}: {@code Map<String,Object>} from {@link NodeExecutionContext#variables()}
 *   <li>{@code businessData}: {@link JsonNode} from {@link NodeExecutionContext#businessData()}
 *   <li>{@code results}: lazy view exposing {@code results['nodeId'].outcome} etc.
 *   <li>{@code ctx}: aggregate view exposing {@code ctx.variables} / {@code ctx.businessData} /
 *       {@code ctx.results}
 * </ul>
 *
 * <p>Legacy syntax preserved for backward compatibility:
 *
 * <ul>
 *   <li>{@code ${varName}}: a single bare identifier reads from {@code variables.varName} (handled
 *       via a regex short-circuit before reaching the EL engine).
 *   <li>{@code ${variables.x}} / {@code ${businessData.a.b}} / {@code ${results.nodeId.outcome}}:
 *       dot-path access (resolved via the custom EL resolvers).
 * </ul>
 */
public final class ConditionEvaluator {

    private static final ExpressionFactory FACTORY = ExpressionFactory.newInstance();

    /** Matches {@code ${identifier}} where identifier is a single Java identifier (no dots). */
    private static final Pattern BARE_IDENT =
            Pattern.compile("^\\$\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}$");

    private ConditionEvaluator() {
        throw new AssertionError("no instances");
    }

    /** Strict variant: {@code null} / blank input returns {@code false}. */
    public static boolean evaluate(String condition, NodeExecutionContext ctx) {
        if (condition == null) {
            return false;
        }
        String trimmed = condition.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return evaluateNonBlank(trimmed, ctx);
    }

    /** Lenient variant: {@code null} / blank input returns {@code true}. */
    public static boolean evaluateOrTrue(String condition, NodeExecutionContext ctx) {
        if (condition == null) {
            return true;
        }
        String trimmed = condition.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return evaluateNonBlank(trimmed, ctx);
    }

    private static boolean evaluateNonBlank(String trimmed, NodeExecutionContext ctx) {
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        // Anything that isn't wrapped in ${...} is not a recognized expression form and is
        // rejected rather than treated as a truthy string, preventing silent misconfiguration.
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return false;
        }

        // Backward-compat short-circuit: bare ${identifier} reads from variables map.
        // This must run before the EL engine because the EL context only registers the four
        // aggregate names (variables/businessData/results/ctx); a plain ${myVar} would be an
        // unresolvable EL variable and would silently evaluate to null -> false.
        var bareMatch = BARE_IDENT.matcher(trimmed);
        if (bareMatch.matches()) {
            String name = bareMatch.group(1);
            // Reserved names are the EL top-level variables themselves; falling through to the EL
            // engine for these lets expressions like ${variables} (unusual but not illegal) work
            // correctly rather than triggering a map lookup on "variables" as a key.
            if (!"variables".equals(name)
                    && !"businessData".equals(name)
                    && !"results".equals(name)
                    && !"ctx".equals(name)) {
                if (ctx == null) {
                    return false;
                }
                Map<String, Object> vars = ctx.variables();
                return toBoolean(vars == null ? null : vars.get(name));
            }
        }

        // Reject empty ${} explicitly (otherwise EL would throw).
        String inner = trimmed.substring(2, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return false;
        }

        try {
            StandardELContext el = new StandardELContext(FACTORY);
            el.addELResolver(new JsonNodeELResolver());
            el.addELResolver(new ResultsELResolver());

            VariableMapper vm = el.getVariableMapper();
            Map<String, Object> vars = ctx == null ? Map.of() : ctx.variables();
            JsonNode biz = ctx == null ? null : ctx.businessData();
            ResultsView results = new ResultsView(ctx);
            CtxView ctxView = new CtxView(vars, biz, results);

            vm.setVariable("variables", FACTORY.createValueExpression(vars, Map.class));
            vm.setVariable("businessData", FACTORY.createValueExpression(biz, Object.class));
            vm.setVariable("results", FACTORY.createValueExpression(results, ResultsView.class));
            vm.setVariable("ctx", FACTORY.createValueExpression(ctxView, CtxView.class));

            ValueExpression ve = FACTORY.createValueExpression(el, trimmed, Object.class);
            Object value = ve.getValue(el);
            return toBoolean(value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Loosely converts an EL evaluation result to a boolean.
     *
     * <p>Rules: {@code null} → false; {@code Boolean} → exact value; {@link Number} → non-zero;
     * {@link String} → case-insensitive {@code "true"}. Everything else → false. The Number branch
     * exists because EL arithmetic expressions (e.g. {@code ${ctx.score gt 5}}) produce numeric
     * results in some EL implementations, and treating non-zero as truthy matches standard
     * JavaScript / EL coercion semantics.
     */
    private static boolean toBoolean(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.doubleValue() != 0;
        }
        if (v instanceof String s) {
            return "true".equalsIgnoreCase(s.trim());
        }
        return false;
    }

    // -------- inner views (public so EL resolvers can reach them) --------

    /** Lazy view over {@link NodeExecutionContext#resultOf(String)} for {@code results['x']}. */
    public static final class ResultsView {
        private final NodeExecutionContext ctx;

        ResultsView(NodeExecutionContext ctx) {
            this.ctx = ctx;
        }

        public NodeResult get(String nodeId) {
            return ctx == null ? null : ctx.resultOf(nodeId);
        }
    }

    /**
     * Aggregate top-level view: exposes {@code ctx.variables} / {@code ctx.businessData} / {@code
     * ctx.results}.
     */
    public static final class CtxView {
        private final Map<String, Object> variables;
        private final JsonNode businessData;
        private final ResultsView results;

        CtxView(Map<String, Object> vars, JsonNode biz, ResultsView res) {
            this.variables = vars;
            this.businessData = biz;
            this.results = res;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public JsonNode getBusinessData() {
            return businessData;
        }

        public ResultsView getResults() {
            return results;
        }
    }

    /** Convenience for the resolvers (kept package-private through public class for visibility). */
    static ELContext newContextForTesting() {
        return new StandardELContext(FACTORY);
    }
}
