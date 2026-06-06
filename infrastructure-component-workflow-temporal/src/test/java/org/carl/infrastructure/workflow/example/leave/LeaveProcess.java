package org.carl.infrastructure.workflow.example.leave;

import static org.carl.infrastructure.workflow.dsl.Dsl.all;
import static org.carl.infrastructure.workflow.dsl.Dsl.any;
import static org.carl.infrastructure.workflow.dsl.Dsl.node;

import java.util.Map;

import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;

/** Static {@link WorkflowDefinition}s used by the leave end-to-end tests. */
final class LeaveProcess {

    private LeaveProcess() {
        throw new AssertionError("no instances");
    }

    /**
     * Linear leave flow with a single {@code approvalTask}:
     *
     * <pre>
     * requestLeave --(SUCCESS)--> leaveApproval
     *   leaveApproval --APPROVED--> onLeave --(SUCCESS)--> completed
     *   leaveApproval --REJECTED--> rejected
     *   leaveApproval --TIMEOUT--> timedOut
     * </pre>
     */
    static WorkflowDefinition singleApprovalFlow() {
        FlowDef flow = Flow.define("leaveV2", "请假流程 V2");
        flow.start("requestLeave");

        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node(
                "leaveApproval",
                BuiltInNodes.approval("manager")
                        .andThen(
                                b ->
                                        b.set("awaitEvent", "approval")
                                                .set("timeoutDuration", "PT24H")));
        flow.node("onLeave", BuiltInNodes.service("notifyManager"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("已拒绝"));
        flow.node("timedOut", b -> b.type(NodeTypes.END_TASK).label("已超时"));

        flow.from("requestLeave").on(Outcomes.SUCCESS).to("leaveApproval");
        flow.from("leaveApproval").on(Outcomes.APPROVED).to("onLeave");
        flow.from("leaveApproval").on(Outcomes.REJECTED).to("rejected");
        flow.from("leaveApproval").on(Outcomes.TIMEOUT).to("timedOut");
        flow.from("onLeave").on(Outcomes.SUCCESS).to("completed");

        return flow.build();
    }

    /**
     * Flow with a {@code taskGroup} (HR + manager co-sign) instead of a single approval node.
     *
     * <pre>
     * requestLeave --(SUCCESS)--> approvals (taskGroup ALL, [hrApproval, managerApproval])
     *   approvals --APPROVED--> onLeave --(SUCCESS)--> completed
     *   approvals --REJECTED--> rejected
     * </pre>
     */
    static WorkflowDefinition coSignFlow() {
        FlowDef flow = Flow.define("leaveCoSignV2", "请假流程会签 V2");
        flow.start("requestLeave");

        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node("onLeave", BuiltInNodes.service("notifyManager"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("已拒绝"));

        flow.from("requestLeave").on(Outcomes.SUCCESS).to("approvals");
        flow.from("approvals")
                .join(
                        all(
                                node("hrApproval", BuiltInNodes.approval("hr")),
                                node("managerApproval", BuiltInNodes.approval("manager"))))
                .on(Outcomes.APPROVED)
                .to("onLeave")
                .on(Outcomes.REJECTED)
                .to("rejected");
        flow.from("onLeave").on(Outcomes.SUCCESS).to("completed");

        return flow.build();
    }

    /**
     * Or-sign variant of {@link #coSignFlow()}: HR and manager both review, but the taskGroup joins
     * with {@code joinRule=ANY} so a single APPROVED short-circuits the rest. Used by the
     * short-circuit test to prove the runtime cancels the still-pending sibling.
     *
     * <pre>
     * requestLeave --(SUCCESS)--> approvals (taskGroup ANY, [hrApproval, managerApproval])
     *   approvals --APPROVED--> onLeave --(SUCCESS)--> completed
     *   approvals --REJECTED--> rejected
     * </pre>
     */
    static WorkflowDefinition coSignAnyFlow() {
        FlowDef flow = Flow.define("leaveCoSignAnyV2", "请假流程或签 V2");
        flow.start("requestLeave");

        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node("onLeave", BuiltInNodes.service("notifyManager"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("已拒绝"));

        flow.from("requestLeave").on(Outcomes.SUCCESS).to("approvals");
        flow.from("approvals")
                .join(
                        any(
                                node("hrApproval", BuiltInNodes.approval("hr")),
                                node("managerApproval", BuiltInNodes.approval("manager"))))
                .on(Outcomes.APPROVED)
                .to("onLeave")
                .on(Outcomes.REJECTED)
                .to("rejected");
        flow.from("onLeave").on(Outcomes.SUCCESS).to("completed");

        return flow.build();
    }

    /**
     * Flow demonstrating a back-edge: when the approval is rejected, control loops back to {@code
     * requestLeave} (re-running the {@code createLeaveRequest} service activity). On approval the
     * flow terminates at {@code done}.
     *
     * <pre>
     * requestLeave --(SUCCESS)--> leaveApproval
     *   leaveApproval --APPROVED--> done
     *   leaveApproval --REJECTED--> requestLeave    (back-edge to start)
     * </pre>
     *
     * The explicit {@code start("requestLeave")} hint is required because the back-edge gives
     * {@code requestLeave} a non-zero in-degree, defeating topology-based start detection.
     */
    static WorkflowDefinition withRejectionLoopFlow() {
        FlowDef flow = Flow.define("leaveRejectionLoopV2", "请假驳回回环 V2");
        flow.start("requestLeave");

        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node(
                "leaveApproval",
                BuiltInNodes.approval("manager").andThen(b -> b.set("awaitEvent", "approval")));
        flow.node("done", b -> b.type(NodeTypes.END_TASK).label("已完成"));

        flow.from("requestLeave").on(Outcomes.SUCCESS).to("leaveApproval");
        flow.from("leaveApproval").on(Outcomes.APPROVED).to("done");
        flow.from("leaveApproval").on(Outcomes.REJECTED).to("requestLeave");

        return flow.build();
    }
}
