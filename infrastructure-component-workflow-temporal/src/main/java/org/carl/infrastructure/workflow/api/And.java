package org.carl.infrastructure.workflow.api;

import java.util.List;

/**
 * An AND node: all children must evaluate to TRUE for this node to be TRUE; any FALSE makes this
 * node FALSE; otherwise UNKNOWN.
 *
 * @see ExprEvaluator
 */
public record And(List<Expr> children) implements Expr {}
