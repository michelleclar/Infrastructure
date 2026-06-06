package org.carl.infrastructure.workflow.spi;

/**
 * Typed counterparts of the built-in node-type strings exposed by {@link NodeTypes}.
 *
 * <p>Use {@code BuiltInNodeType.X} in DSL contexts when you want compile-time type safety and
 * IDE autocomplete:
 *
 * <pre>
 * flow.node("step1", b -&gt; b
 *     .type(BuiltInNodeType.SERVICE_TASK)
 *     .setAll(new ServiceTaskConfig("createOrder", null, null)));
 * </pre>
 *
 * <p>The {@code value()} returned by each enum constant is the same wire-format string as the
 * corresponding {@code NodeTypes} constant. A unit test enforces this equivalence so the two
 * sources never drift.
 */
public enum BuiltInNodeType implements NodeType {

    SERVICE_TASK("serviceTask"),
    APPROVAL_TASK("approvalTask"),
    USER_TASK("userTask"),
    EVENT_TASK("eventTask"),
    TIMER_TASK("timerTask"),
    TASK_GROUP("taskGroup"),
    GATEWAY("gateway"),
    SUB_PROCESS("subProcess"),
    END_TASK("endTask");

    private final String value;

    BuiltInNodeType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
