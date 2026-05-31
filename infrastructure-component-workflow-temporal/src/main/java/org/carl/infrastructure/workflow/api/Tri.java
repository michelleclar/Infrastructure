package org.carl.infrastructure.workflow.api;

/**
 * Three-valued logic used by the approval expression evaluator.
 *
 * <ul>
 *   <li>{@link #TRUE} — step voted APPROVE (or subtree fully approved).
 *   <li>{@link #FALSE} — step voted REJECT (or subtree fully rejected).
 *   <li>{@link #UNKNOWN} — step not yet voted (or ABSTAIN), or subtree still waiting.
 * </ul>
 *
 * @see ExprEvaluator
 */
public enum Tri {
    TRUE,
    FALSE,
    UNKNOWN
}
