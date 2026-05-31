package org.carl.infrastructure.workflow.leave;

import org.carl.infrastructure.workflow.api.ApprovalPolicy;
import org.carl.infrastructure.workflow.api.NodeContext;
import org.carl.infrastructure.workflow.api.ProcessDefinition;
import org.carl.infrastructure.workflow.api.ProcessFlow;
import org.carl.infrastructure.workflow.api.WorkflowActivity;

/**
 * Multi-approver leave-approval process — demonstrates the gather (会签) DSL.
 *
 * <pre>
 *   SUBMITTED -[gather: assignees=ctx.approvers, policy=ctor-given]-+-> APPROVED  (RecordApproval)
 *                                                                   +-> REJECTED  (RecordRejection)
 * </pre>
 *
 * Parameterized by {@code id} and {@code policy} via the constructor so tests can register
 * multiple variants (allApprove, kOfN, etc.) without duplicating the process class.
 */
public class TeamLeaveProcess implements ProcessDefinition<LeaveStatus, LeaveEvent, TeamLeaveCtx> {

    private final String id;
    private final ApprovalPolicy policy;
    private final WorkflowActivity<LeaveStatus, LeaveEvent, TeamLeaveCtx> approvedActivity;
    private final WorkflowActivity<LeaveStatus, LeaveEvent, TeamLeaveCtx> rejectedActivity;

    public TeamLeaveProcess(String id, ApprovalPolicy policy) {
        this.id = id;
        this.policy = policy;
        // Activity names must be unique within a process; we suffix with the process id since the
        // example registers multiple variants in the same JVM and ProcessRegistry namespaces by id.
        this.approvedActivity = new RecordApproval("recordApproval@" + id);
        this.rejectedActivity = new RecordRejection("recordRejection@" + id);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void define(ProcessFlow<LeaveStatus, LeaveEvent, TeamLeaveCtx> flow) {
        flow.gather(LeaveStatus.SUBMITTED)
                .assignees(TeamLeaveCtx::getApprovers)
                .policy(policy)
                .onApproved(LeaveStatus.APPROVED)
                .perform(approvedActivity)
                .onRejected(LeaveStatus.REJECTED)
                .perform(rejectedActivity);
    }

    @Override
    public LeaveStatus startState() {
        return LeaveStatus.SUBMITTED;
    }

    @Override
    public boolean isTerminal(LeaveStatus state) {
        return state == LeaveStatus.APPROVED || state == LeaveStatus.REJECTED;
    }

    @Override
    public Class<LeaveStatus> stateType() {
        return LeaveStatus.class;
    }

    @Override
    public Class<LeaveEvent> eventType() {
        return LeaveEvent.class;
    }

    @Override
    public Class<TeamLeaveCtx> ctxType() {
        return TeamLeaveCtx.class;
    }

    // ── Outcome activities ────────────────────────────────────────────────────

    static class RecordApproval implements WorkflowActivity<LeaveStatus, LeaveEvent, TeamLeaveCtx> {
        private final String name;

        RecordApproval(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void run(NodeContext<LeaveStatus, LeaveEvent, TeamLeaveCtx> in) {
            TeamRecorder.EVENTS.add("approved:" + in.ctx().getId());
        }
    }

    static class RecordRejection
            implements WorkflowActivity<LeaveStatus, LeaveEvent, TeamLeaveCtx> {
        private final String name;

        RecordRejection(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void run(NodeContext<LeaveStatus, LeaveEvent, TeamLeaveCtx> in) {
            TeamRecorder.EVENTS.add("rejected:" + in.ctx().getId());
        }
    }
}
