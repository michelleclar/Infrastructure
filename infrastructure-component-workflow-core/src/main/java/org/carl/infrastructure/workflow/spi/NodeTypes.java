package org.carl.infrastructure.workflow.spi;

/**
 * Canonical built-in node type discriminators (string constants).
 *
 * <p>Used as {@code NodeDefinition.type} values for the built-in node handlers and inside the
 * runtime where a plain string is required (JSON wire format, registry lookups, switch
 * statements).
 *
 * <p>For typed DSL contexts that only need the type discriminator, use the matching {@link
 * BuiltInNodeType} enum constants. For type + config safety, use {@link
 * org.carl.infrastructure.workflow.handlers.BuiltInNodeSpecs}.
 *
 * <pre>
 * flow.node("step1", b -&gt; b.type(BuiltInNodeType.SERVICE_TASK));   // typed type only
 * flow.node("step1", b -&gt; b.type(NodeTypes.SERVICE_TASK));         // string
 * </pre>
 */
public final class NodeTypes {

    public static final String SERVICE_TASK = "serviceTask";
    public static final String APPROVAL_TASK = "approvalTask";
    public static final String USER_TASK = "userTask";
    public static final String EVENT_TASK = "eventTask";
    public static final String TIMER_TASK = "timerTask";
    public static final String TASK_GROUP = "taskGroup";
    public static final String GATEWAY = "gateway";
    public static final String SUB_PROCESS = "subProcess";
    public static final String END_TASK = "endTask";

    private NodeTypes() {
        throw new AssertionError("no instances");
    }
}
