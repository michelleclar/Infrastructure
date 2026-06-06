package org.carl.infrastructure.workflow.interceptor;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

/**
 * Asynchronous interceptor. The runtime dispatches each hook to a Temporal Activity so
 * implementations can perform I/O safely.
 *
 * <p>Use cases: audit log writes to DB, external event publishing (Kafka/Pulsar), HTTP callbacks,
 * metric reporters.
 *
 * <p>Trade-offs:
 *
 * <ul>
 *   <li>Each hook invocation adds one Activity execution and its history events.
 *   <li>Ordering between async hooks is not strictly guaranteed.
 *   <li>Hook errors do not fail the workflow by default; configure activity retry policy on the
 *       runtime side.
 * </ul>
 */
public interface AsyncInterceptor extends WorkflowInterceptor {

    default void onWorkflowStart(InterceptorContext ctx) {}

    default void onWorkflowEnd(InterceptorContext ctx, NodeResult terminalResult) {}

    default void onNodeEnter(InterceptorContext ctx, NodeDefinition node) {}

    default void onNodeExit(InterceptorContext ctx, NodeDefinition node, NodeResult result) {}

    default void onNodeError(InterceptorContext ctx, NodeDefinition node, String errorMessage) {}

    default void onEvent(InterceptorContext ctx, WorkflowEvent event) {}

    default void onCompensate(
            InterceptorContext ctx, NodeDefinition node, NodeResult originalResult) {}
}
