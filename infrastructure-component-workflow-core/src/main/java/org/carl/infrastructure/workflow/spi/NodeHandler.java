package org.carl.infrastructure.workflow.spi;

import org.carl.infrastructure.workflow.definition.NodeResult;

import java.util.Set;

/**
 * Pluggable node-type handler. The workflow engine only knows the {@code type} string; concrete
 * execution semantics are provided by registered {@code NodeHandler} implementations.
 *
 * <p>The runtime invokes a handler to:
 *
 * <ul>
 *   <li>on entry to a node: decide what the runtime should do next via {@link
 *       #run(NodeExecutionContext, Object)};
 *   <li>on event arrival: filter via {@link #canAccept(NodeExecutionContext, WorkflowEvent,
 *       Object)} and decide the outcome via {@link #onEvent(NodeExecutionContext, WorkflowEvent,
 *       Object)};
 *   <li>on rollback: optionally undo side-effects via {@link #compensate(NodeExecutionContext,
 *       Object, NodeResult)} when {@link #compensable()} returns {@code true}.
 * </ul>
 *
 * <h2>Determinism contract</h2>
 *
 * <p><strong>Implementations must be deterministic.</strong> The runtime (Temporal) replays
 * handlers during workflow recovery; non-deterministic behavior breaks replay and corrupts the
 * workflow state machine.
 *
 * <p>Inside {@link #run(NodeExecutionContext, Object)}, {@link #onEvent(NodeExecutionContext,
 * WorkflowEvent, Object)}, {@link #canAccept(NodeExecutionContext, WorkflowEvent, Object)}, {@link
 * #compensable()}, {@link #compensate(NodeExecutionContext, Object, NodeResult)}, {@link
 * #outcomes()}, {@link #configType()} and {@link #type()}, implementations MUST NOT:
 *
 * <ul>
 *   <li>read wall-clock time ({@code System.currentTimeMillis}, {@code Instant.now}, {@code new
 *       Date()}, {@code LocalDateTime.now}, etc.);
 *   <li>perform I/O (network, file system, database, message queues);
 *   <li>use non-deterministic randomness ({@code Math.random}, {@code UUID.randomUUID}, {@code
 *       SecureRandom});
 *   <li>block the calling thread ({@code Thread.sleep}, latches, {@code Future.get} with timeouts);
 *   <li>read mutable shared state (mutable statics, environment variables, system properties);
 *   <li>throw out of {@code run()}/{@code onEvent()} for non-fatal cases — return {@link
 *       NodeResult#failed(String)} instead.
 * </ul>
 *
 * <p>If your node needs IO, time, or randomness, return {@link NodeResult#waiting()} together with
 * an {@code ACTIVITY} intent (see {@code RuntimeIntents.ACTIVITY}) and let the runtime invoke the
 * activity. Activities execute <em>outside</em> the deterministic replay context where IO is safe.
 *
 * <h2>Compensation</h2>
 *
 * <p>{@link #compensable()} declares whether the runtime should track this node for saga
 * compensation. {@link #compensate(NodeExecutionContext, Object, NodeResult)} is invoked during
 * rollback; the same determinism rules apply (use {@code ACTIVITY} intents for any IO).
 *
 * <h2>Verification</h2>
 *
 * <p>A best-effort determinism lint is applied automatically: {@link
 * NodeHandlerRegistry#register(NodeHandler)} always calls {@link
 * DeterminismGuard#assertPure(Class)} before storing the handler. Built-in (trusted) handlers must
 * use {@link NodeHandlerRegistry#registerBuiltIn(NodeHandler)} to bypass the check. {@link
 * NodeHandlerRegistry#strict()} is retained only for backward compatibility and is a no-op. Real
 * safety still depends on developer discipline and code review; the lint catches obvious cases
 * only.
 *
 * <h2>Examples</h2>
 *
 * <ul>
 *   <li>{@code ServiceTaskHandler} — IO via {@code ACTIVITY} intent.
 *   <li>{@code ApprovalTaskHandler} — wait for signal via {@code AWAIT_EVENT} intent.
 *   <li>{@code GatewayHandler} — pure routing.
 * </ul>
 *
 * @param <C> handler-specific configuration type; not part of the serialized workflow definition
 *     (definitions store {@code JsonNode} and the runtime converts via {@link #configType()}).
 */
public interface NodeHandler<C> {

    /**
     * The node type string this handler implements. Must return a stable, deterministic value
     * (typically a {@code static final} constant).
     */
    String type();

    /**
     * Concrete configuration class used to bind the node's {@code JsonNode config} to a typed value
     * before {@link #run(NodeExecutionContext, Object)} is invoked. Must return a stable class
     * reference.
     */
    Class<C> configType();

    /**
     * Closed set of outcome strings this handler can return via {@link NodeResult#outcome()}. The
     * runtime uses this set for definition validation against outgoing edges. Must return a stable,
     * deterministic set (typically a {@code Set.of(...)} constant).
     */
    Set<String> outcomes();

    /**
     * Invoked once when the workflow enters this node. Return {@link NodeResult#completed(String)}
     * to advance the workflow along the matching outcome edge, or {@link NodeResult#waiting()} —
     * optionally with a {@code RuntimeIntents} payload — to suspend until an event arrives.
     *
     * <p><strong>Must be deterministic.</strong> See the class-level contract.
     */
    NodeResult run(NodeExecutionContext ctx, C config);

    /**
     * Cheap filter invoked when an external or internal event arrives while this node is the
     * current/active node. Return {@code true} to forward the event to {@link
     * #onEvent(NodeExecutionContext, WorkflowEvent, Object)}.
     *
     * <p><strong>Must be deterministic and side-effect free.</strong> Implementations should
     * typically inspect only {@code event.name()} and small payload fields.
     *
     * <p><strong>Note:</strong> Implementations should read the event via the {@code event}
     * parameter rather than {@code ctx.currentEvent()}, since during batch canAccept evaluation the
     * context's current event may not match the candidate.
     */
    default boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, C config) {
        return false;
    }

    /**
     * Invoked after {@link #canAccept(NodeExecutionContext, WorkflowEvent, Object)} returns {@code
     * true}. Return {@link NodeResult#completed(String)} to produce an outcome and route along the
     * matching edge, or {@link NodeResult#waiting()} to keep waiting for more events.
     *
     * <p><strong>Must be deterministic.</strong> See the class-level contract.
     */
    default NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, C config) {
        return NodeResult.waiting();
    }

    /**
     * Declares whether the runtime should track this node for saga-style compensation. Must return
     * a stable value across invocations for the same handler instance.
     */
    default boolean compensable() {
        return false;
    }

    /**
     * Invoked during compensation/rollback to undo this node's effects. Same determinism rules as
     * {@link #run(NodeExecutionContext, Object)} apply; return {@link NodeResult#waiting()} with an
     * {@code ACTIVITY} intent to route IO through the runtime rather than performing it inline.
     * Return {@link NodeResult#completed(String)} (any outcome) when no compensating action is
     * required. The default no-op returns {@link NodeResult#completed(Outcomes#SUCCESS)}.
     */
    default NodeResult compensate(NodeExecutionContext ctx, C config, NodeResult completedResult) {
        return NodeResult.completed(Outcomes.SUCCESS);
    }
}
