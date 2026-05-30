package org.carl.infrastructure.workflowv2.leave;

import org.carl.infrastructure.workflowv2.api.ProcessDefinition;
import org.carl.infrastructure.workflowv2.api.ProcessFlow;

import java.time.Duration;

/**
 * Leave-approval process:
 *
 * <pre>
 *   SUBMITTED --APPROVE[approver!=null]--> APPROVED   (DeductBalanceActivity: retry + compensation)
 *   APPROVED  --CONFIRM--> DONE                        (WriteRecordActivity)
 *   SUBMITTED --REJECT--> REJECTED                     (routing-only)
 *   SUBMITTED --(24h timeout)=TIMEOUT--> REJECTED      (routing-only)
 * </pre>
 */
public class LeaveProcess implements ProcessDefinition<LeaveStatus, LeaveEvent, LeaveCtx> {

    @Override
    public String id() {
        return "leave";
    }

    @Override
    public void define(ProcessFlow<LeaveStatus, LeaveEvent, LeaveCtx> flow) {
        // Class form: reusable WorkflowActivity — owns name/timeout/retry/compensate
        flow.from(LeaveStatus.SUBMITTED)
                .to(LeaveStatus.APPROVED)
                .on(LeaveEvent.APPROVE)
                .when(ctx -> ctx.getApprover() != null)
                .perform(new DeductBalanceActivity());

        // Class form for the confirm step
        flow.from(LeaveStatus.APPROVED)
                .to(LeaveStatus.DONE)
                .on(LeaveEvent.CONFIRM)
                .perform(new WriteRecordActivity());

        // Routing-only transitions: no activity; recorded immediately, no closer needed
        flow.from(LeaveStatus.SUBMITTED)
                .to(LeaveStatus.REJECTED)
                .on(LeaveEvent.REJECT);

        flow.from(LeaveStatus.SUBMITTED)
                .to(LeaveStatus.REJECTED)
                .on(LeaveEvent.TIMEOUT);
    }

    @Override
    public LeaveStatus startState() {
        return LeaveStatus.SUBMITTED;
    }

    @Override
    public boolean isTerminal(LeaveStatus state) {
        return state == LeaveStatus.DONE || state == LeaveStatus.REJECTED;
    }

    @Override
    public Duration awaitTimeout(LeaveStatus state, LeaveCtx ctx) {
        return state == LeaveStatus.SUBMITTED ? Duration.ofHours(24) : null;
    }

    @Override
    public LeaveEvent onTimeout(LeaveStatus state) {
        return state == LeaveStatus.SUBMITTED ? LeaveEvent.TIMEOUT : null;
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
    public Class<LeaveCtx> ctxType() {
        return LeaveCtx.class;
    }
}
