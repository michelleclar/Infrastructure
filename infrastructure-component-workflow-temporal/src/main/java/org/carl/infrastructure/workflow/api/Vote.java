package org.carl.infrastructure.workflow.api;

/**
 * A single approver's vote in a multi-approver gathering state.
 *
 * <p>Plain mutable class (not a {@code record}) for Jackson DataConverter compatibility — Temporal
 * deserializes vote signals using the same conventions as the business context type.
 */
public class Vote {

    private String approver;
    private Decision decision;
    private String comment;

    public Vote() {}

    public Vote(String approver, Decision decision) {
        this(approver, decision, null);
    }

    public Vote(String approver, Decision decision, String comment) {
        this.approver = approver;
        this.decision = decision;
        this.comment = comment;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
