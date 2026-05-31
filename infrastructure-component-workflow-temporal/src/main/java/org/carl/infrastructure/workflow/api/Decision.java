package org.carl.infrastructure.workflow.api;

/** A single approver's decision in an approval state. */
public enum Decision {
    APPROVE,
    REJECT,
    /** Voted but neither for nor against; treated as UNKNOWN in expression evaluation. */
    ABSTAIN,
    /**
     * Clears the previous vote for this step, returning it to UNKNOWN. The engine removes the step's
     * entry from the vote map and re-evaluates the expression.
     */
    SENDBACK
}
