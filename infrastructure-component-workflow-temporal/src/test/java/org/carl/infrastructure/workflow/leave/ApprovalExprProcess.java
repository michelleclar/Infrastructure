package org.carl.infrastructure.workflow.leave;

import org.carl.infrastructure.workflow.api.Expr;
import org.carl.infrastructure.workflow.api.NodeContext;
import org.carl.infrastructure.workflow.api.ProcessDefinition;
import org.carl.infrastructure.workflow.api.ProcessFlow;
import org.carl.infrastructure.workflow.api.WorkflowActivity;

/**
 * Parameterized approval process for expression-based tests. Each test instance provides its own
 * expression tree and (optionally) approved/rejected outcome activities.
 *
 * <pre>
 *   SUBMITTED -[approval: expr=ctor-given]-> APPROVED
 *                                           -> REJECTED
 * </pre>
 */
public class ApprovalExprProcess
        implements ProcessDefinition<LeaveStatus, LeaveEvent, ApprovalExprCtx> {

    private final String id;
    private final Expr expr;
    private final WorkflowActivity<LeaveStatus, LeaveEvent, ApprovalExprCtx> approvedActivity;
    private final WorkflowActivity<LeaveStatus, LeaveEvent, ApprovalExprCtx> rejectedActivity;

    /** Simple constructor with no outcome activities. */
    public ApprovalExprProcess(String id, Expr expr) {
        this(id, expr, null, null);
    }

    /** Full constructor with optional outcome activities. */
    public ApprovalExprProcess(
            String id,
            Expr expr,
            WorkflowActivity<LeaveStatus, LeaveEvent, ApprovalExprCtx> approvedActivity,
            WorkflowActivity<LeaveStatus, LeaveEvent, ApprovalExprCtx> rejectedActivity) {
        this.id = id;
        this.expr = expr;
        this.approvedActivity = approvedActivity;
        this.rejectedActivity = rejectedActivity;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void define(ProcessFlow<LeaveStatus, LeaveEvent, ApprovalExprCtx> flow) {
        var approvedBuilder =
                flow.approval(LeaveStatus.SUBMITTED)
                        .expr(expr)
                        .onApproved(LeaveStatus.APPROVED);
        if (approvedActivity != null) {
            approvedBuilder.perform(approvedActivity);
        }
        var rejectedBuilder = approvedBuilder.onRejected(LeaveStatus.REJECTED);
        if (rejectedActivity != null) {
            rejectedBuilder.perform(rejectedActivity);
        }
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
    public Class<ApprovalExprCtx> ctxType() {
        return ApprovalExprCtx.class;
    }

    // ── Built-in activity types for test convenience ──────────────────────────

    /** Activity that records "approved:<ctx.id>" in ApprovalRecorder. */
    public static final class RecordApproved
            implements WorkflowActivity<LeaveStatus, LeaveEvent, ApprovalExprCtx> {

        private final String name;

        public RecordApproved(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void run(NodeContext<LeaveStatus, LeaveEvent, ApprovalExprCtx> in) {
            ApprovalRecorder.EVENTS.add("approved:" + in.ctx().getId());
        }
    }

    /** Activity that records "rejected:<ctx.id>" in ApprovalRecorder. */
    public static final class RecordRejected
            implements WorkflowActivity<LeaveStatus, LeaveEvent, ApprovalExprCtx> {

        private final String name;

        public RecordRejected(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void run(NodeContext<LeaveStatus, LeaveEvent, ApprovalExprCtx> in) {
            ApprovalRecorder.EVENTS.add("rejected:" + in.ctx().getId());
        }
    }

    /** Hook activity that records an event string in ApprovalRecorder. */
    public static final class RecordHook
            implements WorkflowActivity<LeaveStatus, LeaveEvent, ApprovalExprCtx> {

        private final String name;
        private final String tag;

        public RecordHook(String name, String tag) {
            this.name = name;
            this.tag = tag;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void run(NodeContext<LeaveStatus, LeaveEvent, ApprovalExprCtx> in) {
            ApprovalRecorder.EVENTS.add(tag + ":" + in.step());
        }
    }
}
