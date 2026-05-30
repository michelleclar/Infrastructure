package org.carl.infrastructure.workflowv2.leave;

import org.carl.infrastructure.workflowv2.api.NodeContext;
import org.carl.infrastructure.workflowv2.api.RetryPolicy;
import org.carl.infrastructure.workflowv2.api.WorkflowActivity;

import java.time.Duration;

/**
 * Deducts the leave balance when the request is approved.
 * Compensable: rolls back with {@link #compensate} if a later step fails.
 */
public class DeductBalanceActivity
        implements WorkflowActivity<LeaveStatus, LeaveEvent, LeaveCtx> {

    @Override
    public String name() {
        return "deductBalance";
    }

    @Override
    public void run(NodeContext<LeaveStatus, LeaveEvent, LeaveCtx> in) {
        int attempt = Recorder.DEDUCT_ATTEMPTS.incrementAndGet();
        if (attempt <= Recorder.deductFailTimes.get()) {
            throw new RuntimeException("deductBalance transient failure, attempt " + attempt);
        }
        Recorder.EVENTS.add("deductBalance");
    }

    @Override
    public Duration timeout(LeaveCtx ctx) {
        return Duration.ofSeconds(5);
    }

    @Override
    public RetryPolicy retry(LeaveCtx ctx) {
        return RetryPolicy.of(3);
    }

    @Override
    public boolean compensable() {
        return true;
    }

    @Override
    public void compensate(NodeContext<LeaveStatus, LeaveEvent, LeaveCtx> in) {
        // state-aware compensation: knows exactly which transition it is rolling back
        Recorder.EVENTS.add("restoreBalance:" + in.toState() + ":" + in.event());
    }
}
