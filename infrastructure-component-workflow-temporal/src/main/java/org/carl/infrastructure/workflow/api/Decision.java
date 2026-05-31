package org.carl.infrastructure.workflow.api;

/** A single approver's decision for a multi-approver gathering state. */
public enum Decision {
    APPROVE,
    REJECT,
    /** Voted but neither for nor against; affects the effective denominator in some policies. */
    ABSTAIN
}
