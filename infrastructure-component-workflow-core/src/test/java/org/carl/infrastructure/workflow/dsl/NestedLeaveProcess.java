package org.carl.infrastructure.workflow.dsl;

import static org.carl.infrastructure.workflow.dsl.Dsl.all;
import static org.carl.infrastructure.workflow.dsl.Dsl.any;
import static org.carl.infrastructure.workflow.dsl.Dsl.node;

import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;

/**
 * Static {@link WorkflowDefinition} factory for nested-taskGroup tests.
 *
 * <p>Scenario: HR AND (manager OR CFO) must approve.
 *
 * <pre>
 * requestLeave --(SUCCESS)--> approvals (taskGroup ALL)
 *   approvals
 *     ├── hrApproval (approvalTask, "hr")
 *     └── managementApproval (taskGroup ANY)
 *         ├── managerApproval (approvalTask, "manager")
 *         └── cfoApproval    (approvalTask, "cfo")
 *   approvals --APPROVED--> onLeave --(SUCCESS)--> completed
 *   approvals --REJECTED--> rejected
 * </pre>
 */
final class NestedLeaveProcess {

    private NestedLeaveProcess() {
        throw new AssertionError("no instances");
    }

    /**
     * Nested approval flow: outer ALL gate (HR + management-layer), where management-layer is itself
     * an ANY gate (manager OR CFO).
     */
    static WorkflowDefinition nestedApprovalFlow() {
        FlowDef flow = Flow.define("leaveNestedApproval", "请假嵌套审批流程");
        flow.start("requestLeave");

        flow.node("requestLeave", BuiltInNodes.service("createLeaveRequest")
                .andThen(b -> b.set("activityInput", java.util.Map.of("employeeId", "alice"))));
        flow.node("onLeave", BuiltInNodes.service("notifyManager"));
        flow.endNode("completed");
        flow.endNode("rejected");

        // Nested taskGroup: management-layer ANY (manager OR CFO)
        JoinSpec managementLayer = any(
                node("managerApproval", BuiltInNodes.approval("manager")),
                node("cfoApproval",     BuiltInNodes.approval("cfo")));

        // Outer taskGroup: ALL (HR AND management-layer)
        flow.from("requestLeave").on(Outcomes.SUCCESS).to("approvals");
        flow.from("approvals")
                .join(all(
                        node("hrApproval", BuiltInNodes.approval("hr")),
                        node("managementApproval",
                                new NodeConfig(NodeTypes.TASK_GROUP, java.util.Map.of()),
                                managementLayer)))
                .on(Outcomes.APPROVED).to("onLeave")
                .on(Outcomes.REJECTED).to("rejected");
        flow.from("onLeave").on(Outcomes.SUCCESS).to("completed");

        return flow.build();
    }
}
