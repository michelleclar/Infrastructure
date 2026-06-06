package org.carl.infrastructure.workflow.spi;

/**
 * Canonical built-in node type discriminators.
 *
 * <p>Used as {@code NodeDefinition.type} values for the built-in node handlers.
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
