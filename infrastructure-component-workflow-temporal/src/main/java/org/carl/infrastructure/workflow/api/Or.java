package org.carl.infrastructure.workflow.api;

import java.util.List;

/**
 * An OR node: any TRUE child makes this node TRUE; if all children are FALSE this node is FALSE;
 * otherwise UNKNOWN.
 *
 * @see ExprEvaluator
 */
public record Or(List<Expr> children) implements Expr {}
