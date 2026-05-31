package org.carl.infrastructure.workflow.api;

import java.util.Arrays;
import java.util.List;

/**
 * Static factory methods for building {@link Expr} trees. Designed for static import:
 *
 * <pre>
 * import static org.carl.infrastructure.workflow.api.Exprs.*;
 *
 * flow.approval(SUBMITTED)
 *     .expr(and(
 *         step("manager").before(new NotifyApprover()),
 *         or(step("legal"), step("compliance")),
 *         step("ceo")))
 *     .onApproved(APPROVED)
 *     .onRejected(REJECTED);
 * </pre>
 */
public final class Exprs {

    private Exprs() {}

    /** Create a named leaf step node. Use {@link Step#before}, {@link Step#after}, and
     * {@link Step#onComplete} to attach hook activities. */
    public static Step step(String name) {
        return new Step(name);
    }

    /** Create an AND node: all children must be TRUE for the result to be TRUE. */
    public static Expr and(Expr... children) {
        return new And(List.copyOf(Arrays.asList(children)));
    }

    /** Create an OR node: any TRUE child makes the result TRUE. */
    public static Expr or(Expr... children) {
        return new Or(List.copyOf(Arrays.asList(children)));
    }

    /**
     * Create an AT-LEAST-k node: at least {@code k} children must be TRUE for the result to be
     * TRUE.
     */
    public static Expr atLeast(int k, Expr... children) {
        return new AtLeast(k, List.copyOf(Arrays.asList(children)));
    }
}
