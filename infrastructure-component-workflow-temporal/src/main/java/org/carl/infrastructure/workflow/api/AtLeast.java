package org.carl.infrastructure.workflow.api;

import java.util.List;

/**
 * An AT-LEAST-k node: if ≥k children are TRUE, this node is TRUE; if more than (n−k) children are
 * FALSE (i.e., k TRUE can no longer be achieved), this node is FALSE; otherwise UNKNOWN.
 *
 * @see ExprEvaluator
 */
public record AtLeast(int k, List<Expr> children) implements Expr {}
