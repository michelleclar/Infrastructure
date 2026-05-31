package org.carl.infrastructure.workflow.api;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Decides the outcome of a multi-approver gathering state given the current vote tally.
 *
 * <p><b>Determinism contract:</b> {@link #evaluate} is called inside <em>workflow</em> code on
 * every new vote, so it MUST be a pure function — no IO, no clock, no randomness. The same inputs
 * MUST produce the same {@link Outcome} across Temporal replays.
 *
 * <p>Built-in policies via the static factories below; custom policies are implemented as the
 * single abstract method.
 */
@FunctionalInterface
public interface ApprovalPolicy {

    /** What the engine should do given the current voting state. */
    enum Outcome {
        /** Keep waiting for more votes. */
        WAIT,
        APPROVED,
        REJECTED
    }

    /**
     * @param assignees the (immutable) set of approvers expected to vote, computed once at state
     *     entry from ctx
     * @param votes the votes received so far, keyed by approver id (last-write-wins; only contains
     *     approvers that are in {@code assignees} — non-assignee votes are dropped by the engine
     *     before they reach this method)
     */
    Outcome evaluate(Set<String> assignees, Map<String, Vote> votes);

    // ── Built-in policies ────────────────────────────────────────────────────

    /**
     * All assignees must {@link Decision#APPROVE}. Any {@link Decision#REJECT} short-circuits to
     * {@link Outcome#REJECTED}. ABSTAINs neither approve nor reject — gathering continues until
     * every assignee has voted with APPROVE (or anyone rejects).
     */
    static ApprovalPolicy allApprove() {
        return (assignees, votes) -> {
            if (assignees.isEmpty()) {
                throw new IllegalStateException("allApprove() requires at least one assignee");
            }
            int approves = 0;
            for (String a : assignees) {
                Vote v = votes.get(a);
                if (v == null) continue;
                if (v.getDecision() == Decision.REJECT) {
                    return Outcome.REJECTED;
                }
                if (v.getDecision() == Decision.APPROVE) {
                    approves++;
                }
            }
            return approves == assignees.size() ? Outcome.APPROVED : Outcome.WAIT;
        };
    }

    /**
     * Any {@link Decision#APPROVE} short-circuits to {@link Outcome#APPROVED}. If every assignee
     * has voted and none approved, {@link Outcome#REJECTED}. ABSTAIN counts as "voted" but does
     * not approve.
     */
    static ApprovalPolicy anyApprove() {
        return (assignees, votes) -> {
            if (assignees.isEmpty()) {
                throw new IllegalStateException("anyApprove() requires at least one assignee");
            }
            int votedCount = 0;
            for (String a : assignees) {
                Vote v = votes.get(a);
                if (v == null) continue;
                votedCount++;
                if (v.getDecision() == Decision.APPROVE) {
                    return Outcome.APPROVED;
                }
            }
            return votedCount == assignees.size() ? Outcome.REJECTED : Outcome.WAIT;
        };
    }

    /**
     * {@code k} APPROVEs → {@link Outcome#APPROVED} (early termination). Once enough REJECTs make
     * reaching {@code k} impossible (i.e. {@code n - rejects < k} where {@code n = |assignees|}),
     * the outcome is {@link Outcome#REJECTED} — also early termination.
     */
    static ApprovalPolicy kOfN(int k) {
        if (k < 1) {
            throw new IllegalArgumentException("kOfN requires k >= 1");
        }
        return (assignees, votes) -> {
            int n = assignees.size();
            if (k > n) {
                throw new IllegalStateException(
                        "kOfN(" + k + ") cannot be satisfied with " + n + " assignees");
            }
            int approves = 0;
            int rejects = 0;
            for (String a : assignees) {
                Vote v = votes.get(a);
                if (v == null) continue;
                if (v.getDecision() == Decision.APPROVE) approves++;
                else if (v.getDecision() == Decision.REJECT) rejects++;
            }
            if (approves >= k) return Outcome.APPROVED;
            if (n - rejects < k) return Outcome.REJECTED;
            return Outcome.WAIT;
        };
    }

    /**
     * Strict majority of assignees APPROVE → {@link Outcome#APPROVED}; once a majority is no longer
     * possible (i.e. {@code n - rejects <= n/2}), {@link Outcome#REJECTED}.
     */
    static ApprovalPolicy majority() {
        return (assignees, votes) -> {
            int n = assignees.size();
            if (n == 0) {
                throw new IllegalStateException("majority() requires at least one assignee");
            }
            int threshold = n / 2 + 1; // strict majority
            int approves = 0;
            int rejects = 0;
            for (String a : assignees) {
                Vote v = votes.get(a);
                if (v == null) continue;
                if (v.getDecision() == Decision.APPROVE) approves++;
                else if (v.getDecision() == Decision.REJECT) rejects++;
            }
            if (approves >= threshold) return Outcome.APPROVED;
            if (n - rejects < threshold) return Outcome.REJECTED;
            return Outcome.WAIT;
        };
    }

    // Convenience for engine: ensure assignees is a Set (immutable view) when passed in.
    @SuppressWarnings("unused")
    static Set<String> toSet(Collection<String> assignees) {
        return Set.copyOf(assignees);
    }
}
