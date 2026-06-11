package org.carl.infrastructure.workflow.runtime;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.time.Duration;
import java.util.Map;

/**
 * Side-effect port for {@link WorkflowInterpreter}.
 *
 * <p>The interpreter owns all <em>orchestration decisions</em> (node loop, routing, WAITING
 * dispatch, saga ordering, deterministic hooks) and is pure with respect to Temporal — it never
 * touches {@code Workflow.*}/{@code Async}/{@code ActivityStub}. Every actual side effect goes
 * through this port. {@link GenericWorkflowImpl} implements it with real Temporal APIs; a
 * {@code FakeRuntimeOps} implements it for interpreter unit tests that run on a plain JVM with no
 * Temporal runtime.
 */
interface RuntimeOps {

    /** Invoke a business activity by name, blocking until it completes. */
    ActivityResult runActivity(String activityName, Map<String, Object> input);

    /** Durable timer: block for the given duration. */
    void sleep(Duration duration);

    /**
     * Block until an event matching {@code matcher} arrives, or {@code timeout} (null = no timeout)
     * elapses.
     */
    AwaitOutcome awaitEvent(EventMatcher matcher, Duration timeout);

    /** Run a child workflow, blocking until it completes. */
    WorkflowResult runSubProcess(
            WorkflowDefinition childDef, Map<String, Object> input, String idHint);

    /**
     * Run a {@code taskGroup}: the adapter performs concurrent fan-out + cancellation; the supplied
     * callbacks re-enter the interpreter to execute each child node and to ask the taskGroup handler
     * whether the join condition is met. (Stage 19a keeps the concurrency primitive in the adapter;
     * 19b will finalise this port.)
     */
    NodeResult runTaskGroup(
            Map<String, Object> intentPayload,
            NodeDefinition parentNode,
            String parentQualifier,
            TaskGroupChildExecutor childExecutor,
            TaskGroupJoiner joiner);

    /** Dispatch an async interceptor hook (fire-and-forget; promise collected internally). */
    void emitAsyncHook(AsyncHookInvocation invocation);

    /** Drain all outstanding async hooks before a terminal return (best-effort). */
    void awaitAsyncHooks();

    /** Archive the terminal result if archival is enabled (fire-and-forget). */
    void archive(WorkflowResult result);

    /** Replay-safe warning log (no-op acceptable in test fakes). */
    void warn(String message);

    /** Outcome of {@link #awaitEvent}: a matched event, a timeout, or a spurious wake (neither). */
    record AwaitOutcome(WorkflowEvent matched, boolean timedOut) {}

    /** Pure predicate (wraps {@code handler.canAccept}) used to select a signal from the queue. */
    @FunctionalInterface
    interface EventMatcher {
        boolean matches(WorkflowEvent event);
    }

    /** Re-enters the interpreter to execute one taskGroup child node. */
    @FunctionalInterface
    interface TaskGroupChildExecutor {
        NodeResult execute(NodeDefinition childNode, String qualifier);
    }

    /**
     * Asks the taskGroup handler to fold one completed child into the aggregate result; a
     * non-WAITING return means the group short-circuited.
     */
    @FunctionalInterface
    interface TaskGroupJoiner {
        NodeResult onChildCompleted();
    }
}
