package org.carl.infrastructure.workflow.api;

import java.util.Map;

/**
 * Pure (stateless) evaluator for approval expression trees.
 *
 * <p><b>Determinism contract:</b> this class contains no mutable state and performs no IO. It must
 * only be called from workflow code (the decision loop); that is already enforced by usage in
 * {@code GenericWorkflow}.
 *
 * <p>Leaf evaluation ({@link #evalLeaf}):
 *
 * <ul>
 *   <li>APPROVE → {@link Tri#TRUE}
 *   <li>REJECT → {@link Tri#FALSE}
 *   <li>null / ABSTAIN → {@link Tri#UNKNOWN}
 *   <li>(SENDBACK is handled by the engine before reaching evaluation — it removes the entry)
 * </ul>
 *
 * <p>Inner node evaluation:
 *
 * <ul>
 *   <li>AND: any FALSE → FALSE; all TRUE → TRUE; else UNKNOWN.
 *   <li>OR: any TRUE → TRUE; all FALSE → FALSE; else UNKNOWN.
 *   <li>AtLeast(k): ≥k TRUE → TRUE; >(n−k) FALSE → FALSE; else UNKNOWN.
 * </ul>
 */
public final class ExprEvaluator {

    private ExprEvaluator() {}

    /**
     * Evaluate the entire expression tree against the current vote map.
     *
     * @param expr the expression tree (not null)
     * @param votes map from step name → Decision (missing means no vote yet)
     * @return the three-valued result
     */
    public static Tri evaluate(Expr expr, Map<String, Decision> votes) {
        if (expr instanceof Step s) {
            return evalLeaf(votes.get(s.name()));
        } else if (expr instanceof And and) {
            boolean hasUnknown = false;
            for (Expr child : and.children()) {
                Tri t = evaluate(child, votes);
                if (t == Tri.FALSE) return Tri.FALSE;
                if (t == Tri.UNKNOWN) hasUnknown = true;
            }
            return hasUnknown ? Tri.UNKNOWN : Tri.TRUE;
        } else if (expr instanceof Or or) {
            boolean hasUnknown = false;
            for (Expr child : or.children()) {
                Tri t = evaluate(child, votes);
                if (t == Tri.TRUE) return Tri.TRUE;
                if (t == Tri.UNKNOWN) hasUnknown = true;
            }
            return hasUnknown ? Tri.UNKNOWN : Tri.FALSE;
        } else if (expr instanceof AtLeast al) {
            int trueCount = 0;
            int falseCount = 0;
            int n = al.children().size();
            for (Expr child : al.children()) {
                Tri t = evaluate(child, votes);
                if (t == Tri.TRUE) trueCount++;
                else if (t == Tri.FALSE) falseCount++;
            }
            if (trueCount >= al.k()) return Tri.TRUE;
            if (falseCount > n - al.k()) return Tri.FALSE;
            return Tri.UNKNOWN;
        } else {
            throw new IllegalArgumentException("Unknown Expr type: " + expr.getClass());
        }
    }

    /**
     * Evaluate a single leaf decision to Tri.
     *
     * @param decision the current Decision for this step, or null if not yet voted
     * @return Tri.TRUE for APPROVE, Tri.FALSE for REJECT, Tri.UNKNOWN otherwise
     */
    public static Tri evalLeaf(Decision decision) {
        if (decision == null) return Tri.UNKNOWN;
        return switch (decision) {
            case APPROVE -> Tri.TRUE;
            case REJECT -> Tri.FALSE;
            default -> Tri.UNKNOWN; // ABSTAIN or SENDBACK (SENDBACK clears the entry, so this is defensive)
        };
    }
}
