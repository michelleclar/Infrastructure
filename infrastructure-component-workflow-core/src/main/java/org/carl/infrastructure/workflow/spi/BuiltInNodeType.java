package org.carl.infrastructure.workflow.spi;

/**
 * Typed counterparts of the built-in node-type strings exposed by {@link NodeTypes}.
 *
 * <p>Use {@code BuiltInNodeType.X} in DSL contexts when you only need compile-time safety for the
 * type discriminator. Use {@link org.carl.infrastructure.workflow.handlers.BuiltInNodeSpecs} when
 * the config object should be checked with the type.
 *
 * <pre>
 * flow.node("step1", b -&gt; b.type(BuiltInNodeType.SERVICE_TASK));
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
