package org.carl.infrastructure.workflow.api;

/**
 * A boolean expression node in an approval tree.
 *
 * <p>The leaves are {@link Step}s (named votes); the inner nodes are {@link And}, {@link Or}, and
 * {@link AtLeast}. Evaluation is three-valued (see {@link Tri}). The tree is defined once per
 * process at registration time; it is static — no mutable state beyond each {@link Step}'s hook
 * fields, which are also set at definition time.
 *
 * @see Exprs
 * @see ExprEvaluator
 */
public sealed interface Expr permits Step, And, Or, AtLeast {}
