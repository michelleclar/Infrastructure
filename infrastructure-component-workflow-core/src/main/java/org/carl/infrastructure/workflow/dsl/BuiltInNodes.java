package org.carl.infrastructure.workflow.dsl;

import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.function.Consumer;

/**
 * Optional sugar layer producing {@link Consumer Consumer&lt;NodeBuilder&gt;} factories for the
 * canonical built-in node types declared in {@link NodeTypes}.
 *
 * <p>Usage:
 *
 * <pre>
 * import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.*;
 *
 * flow.node("发起请假", service("createLeaveRequest"));
 * flow.node("等待审批", approval("hr"));
 * </pre>
 *
 * <p>Pure sugar — equivalent to writing {@code b -> b.type(...).set(...)} by hand. Business code
 * that supplies its own {@code NodeHandler} can use {@link NodeBuilder} directly without going
 * through this class; nothing here is required for custom types to work.
 */
public final class BuiltInNodes {

    private BuiltInNodes() {
        throw new AssertionError("no instances");
    }

    /** {@link NodeTypes#SERVICE_TASK} configurer that pins the {@code activity} property. */
    public static Consumer<NodeBuilder> service(String activity) {
        return b -> b.type(NodeTypes.SERVICE_TASK).set("activity", activity);
    }

    /** {@link NodeTypes#APPROVAL_TASK} configurer that pins the {@code assignee} property. */
    public static Consumer<NodeBuilder> approval(String assignee) {
        return b -> b.type(NodeTypes.APPROVAL_TASK).set("assignee", assignee);
    }

    /** {@link NodeTypes#USER_TASK} configurer that pins the {@code assignee} property. */
    public static Consumer<NodeBuilder> userTask(String assignee) {
        return b -> b.type(NodeTypes.USER_TASK).set("assignee", assignee);
    }

    /** {@link NodeTypes#EVENT_TASK} configurer that pins the {@code awaitedEvent} property. */
    public static Consumer<NodeBuilder> event(String awaitedEvent) {
        return b -> b.type(NodeTypes.EVENT_TASK).set("awaitedEvent", awaitedEvent);
    }

    /**
     * {@link NodeTypes#TIMER_TASK} configurer that pins the {@code duration} property to the
     * supplied ISO-8601 string.
     */
    public static Consumer<NodeBuilder> timer(String iso8601Duration) {
        return b -> b.type(NodeTypes.TIMER_TASK).set("duration", iso8601Duration);
    }

    /**
     * {@link NodeTypes#GATEWAY} configurer without further properties; callers may chain additional
     * {@code set(...)} calls.
     */
    public static Consumer<NodeBuilder> gateway() {
        return b -> b.type(NodeTypes.GATEWAY);
    }

    /** {@link NodeTypes#SUB_PROCESS} configurer that pins the {@code subWorkflowId} property. */
    public static Consumer<NodeBuilder> subProcess(String subWorkflowId) {
        return b -> b.type(NodeTypes.SUB_PROCESS).set("subWorkflowId", subWorkflowId);
    }

    /** {@link NodeTypes#END_TASK} configurer. */
    public static Consumer<NodeBuilder> endTask() {
        return b -> b.type(NodeTypes.END_TASK);
    }

    /**
     * {@link NodeTypes#TASK_GROUP} configurer (used together with a subsequent {@code
     * from(...).join(...)} call to populate the join spec).
     */
    public static Consumer<NodeBuilder> taskGroup() {
        return b -> b.type(NodeTypes.TASK_GROUP);
    }
}
