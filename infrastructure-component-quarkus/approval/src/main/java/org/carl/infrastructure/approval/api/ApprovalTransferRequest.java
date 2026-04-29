package org.carl.infrastructure.approval.api;

public class ApprovalTransferRequest {
    private String toUserId;
    private String comment;

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
