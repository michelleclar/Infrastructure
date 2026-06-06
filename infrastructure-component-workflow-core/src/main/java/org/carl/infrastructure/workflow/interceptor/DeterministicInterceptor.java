package org.carl.infrastructure.workflow.interceptor;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

/**
 * Synchronous interceptor invoked inline inside the workflow function. <strong>Must be
 * deterministic.</strong>
 *
 * <h2>Determinism contract</h2>
 *
 * Same rules as {@link org.carl.infrastructure.workflow.spi.NodeHandler}: no I/O, no wall-clock
 * time, no randomness, no blocking, no mutable shared state. If you need any of these, use {@link
 * AsyncInterceptor} instead.
 *
 * <p>Use cases: in-memory metric counters held in workflow state, structured logging via Temporal
 * logger (replay-safe), deterministic side-effect-free checks.
 */
public interface DeterministicInterceptor extends WorkflowInterceptor {

    default void onWorkflowStart(InterceptorContext ctx) {}

    default void onWorkflowEnd(InterceptorContext ctx, NodeResult terminalResult) {}

    default void onNodeEnter(InterceptorContext ctx, NodeDefinition node) {}

    default void onNodeExit(InterceptorContext ctx, NodeDefinition node, NodeResult result) {}

    default void onNodeError(InterceptorContext ctx, NodeDefinition node, String errorMessage) {}

    default void onEvent(InterceptorContext ctx, WorkflowEvent event) {}

    default void onCompensate(
            InterceptorContext ctx, NodeDefinition node, NodeResult originalResult) {}
}
