package org.carl.infrastructure.workflowv2.leave;

import org.carl.infrastructure.workflowv2.api.NodeContext;
import org.carl.infrastructure.workflowv2.api.RetryPolicy;
import org.carl.infrastructure.workflowv2.api.WorkflowActivity;

import java.time.Duration;

/** Writes the leave record once the approval is confirmed. */
public class WriteRecordActivity implements WorkflowActivity<LeaveStatus, LeaveEvent, LeaveCtx> {

    @Override
    public String name() {
        return "writeRecord";
    }

    @Override
    public void run(NodeContext<LeaveStatus, LeaveEvent, LeaveCtx> in) {
        if (Recorder.failWriteRecord.get()) {
            throw new RuntimeException("writeRecord failure");
        }
        Recorder.EVENTS.add("writeRecord");
    }

    @Override
    public Duration timeout(LeaveCtx ctx) {
        return Duration.ofSeconds(5);
    }

    @Override
    public RetryPolicy retry(LeaveCtx ctx) {
        return RetryPolicy.of(1);
    }
}
