package org.carl.infrastructure.approval.api;

public class ApprovalStartResponse {
    private long instanceId;

    public ApprovalStartResponse(long instanceId) {
        this.instanceId = instanceId;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
