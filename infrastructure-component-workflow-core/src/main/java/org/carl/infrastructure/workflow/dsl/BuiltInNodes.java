package org.carl.infrastructure.workflow.dsl;

import org.carl.infrastructure.workflow.handlers.ApprovalTaskConfig;
import org.carl.infrastructure.workflow.handlers.BuiltInNodeSpecs;
import org.carl.infrastructure.workflow.handlers.EventTaskConfig;
import org.carl.infrastructure.workflow.handlers.ServiceTaskConfig;
import org.carl.infrastructure.workflow.handlers.SubProcessConfig;
import org.carl.infrastructure.workflow.handlers.TimerTaskConfig;
import org.carl.infrastructure.workflow.handlers.UserTaskConfig;
import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.Map;
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
        return b -> b.config(
                BuiltInNodeSpecs.SERVICE_TASK, new ServiceTaskConfig(activity, null, null));
    }

    /**
     * {@link NodeTypes#SERVICE_TASK} configurer that pins both the {@code activity} and
     * {@code activityInput} properties in one call, avoiding the need for {@code .andThen}.
     *
     * @param activity the activity name
     * @param activityInput the input map passed to the activity; {@code null} is allowed and
     *     equivalent to {@code Map.of()}
     */
    public static Consumer<NodeBuilder> service(String activity, Map<String, Object> activityInput) {
        return b -> b.config(
                BuiltInNodeSpecs.SERVICE_TASK,
                new ServiceTaskConfig(activity, activityInput, null));
    }

    /** {@link NodeTypes#APPROVAL_TASK} configurer that pins the {@code assignee} property. */
    public static Consumer<NodeBuilder> approval(String assignee) {
        return b -> b.config(
                BuiltInNodeSpecs.APPROVAL_TASK, new ApprovalTaskConfig(assignee, null, null));
    }

    /** {@link NodeTypes#USER_TASK} configurer that pins the {@code assignee} property. */
    public static Consumer<NodeBuilder> userTask(String assignee) {
        return b -> b.config(
                BuiltInNodeSpecs.USER_TASK, new UserTaskConfig(assignee, null, null));
    }

    /** {@link NodeTypes#EVENT_TASK} configurer that pins the {@code awaitEvent} property. */
    public static Consumer<NodeBuilder> event(String awaitEvent) {
        return b -> b.config(BuiltInNodeSpecs.EVENT_TASK, new EventTaskConfig(awaitEvent, null));
    }

    /**
     * {@link NodeTypes#TIMER_TASK} configurer that pins the {@code duration} property to the
     * supplied ISO-8601 string.
     */
    public static Consumer<NodeBuilder> timer(String iso8601Duration) {
        return b -> b.config(BuiltInNodeSpecs.TIMER_TASK, new TimerTaskConfig(iso8601Duration));
    }

    /** {@link NodeTypes#SUB_PROCESS} configurer that pins the {@code subWorkflowId} property. */
    public static Consumer<NodeBuilder> subProcess(String subWorkflowId) {
        return b -> b.config(
                BuiltInNodeSpecs.SUB_PROCESS,
                new SubProcessConfig(subWorkflowId, null, null, null));
    }
}
