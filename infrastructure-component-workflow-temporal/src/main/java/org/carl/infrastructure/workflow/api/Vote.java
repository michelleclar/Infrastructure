package org.carl.infrastructure.workflow.api;

/**
 * A single approver's vote in an approval state.
 *
 * <p>Plain mutable class (not a {@code record}) for Jackson DataConverter compatibility — Temporal
 * deserializes vote signals using the same conventions as the business context type.
 *
 * <p>{@code step} identifies which leaf in the expression tree this vote is for (e.g. "manager").
 * {@code approver} is the human/system identity casting the vote (informational; not validated by
 * the engine).
 */
public class Vote {

    private String step;
    private String approver;
    private Decision decision;
    private String comment;

    public Vote() {}

    public Vote(String step, String approver, Decision decision) {
        this(step, approver, decision, null);
    }

    public Vote(String step, String approver, Decision decision, String comment) {
        this.step = step;
        this.approver = approver;
        this.decision = decision;
        this.comment = comment;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
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
