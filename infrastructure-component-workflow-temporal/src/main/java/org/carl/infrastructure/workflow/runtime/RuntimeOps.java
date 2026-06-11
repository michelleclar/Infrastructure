package org.carl.infrastructure.workflow.runtime;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Side-effect port for {@link WorkflowInterpreter}.
 *
 * <p>The interpreter owns all <em>orchestration decisions</em> (node loop, routing, WAITING
 * dispatch, taskGroup join policy, saga ordering, deterministic hooks) and is pure with respect to
 * Temporal — it never touches {@code Workflow.*}/{@code Async}/{@code ActivityStub}. Every actual
 * side effect goes through this port. {@link GenericWorkflowImpl} implements it with real Temporal
 * APIs; a {@code FakeRuntimeOps} implements it for interpreter unit tests that run on a plain JVM
 * with no Temporal runtime.
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
     * Fan out concurrent child tasks and return a {@link TaskGroupScope} the interpreter uses to
     * drive the join loop (await completions, read results, short-circuit cancel). The concurrency
     * primitive ({@code Async}/{@code CancellationScope}) lives in the adapter; the join
     * <em>policy</em> (which children, when to short-circuit) lives in the interpreter.
     */
    TaskGroupScope fanOut(List<Supplier<NodeResult>> childTasks);

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

    /**
     * Handle to a set of concurrently-running taskGroup children. The interpreter drives the join
     * loop over this; the adapter backs it with Temporal {@code Async}/{@code CancellationScope}.
     */
    interface TaskGroupScope {
        int size();

        boolean isChildCompleted(int index);

        /** Block until at least one not-yet-collected child completes. */
        void awaitAny();

        /** Final result of a child (Temporal cancellation → cancelled, other throw → failed). */
        NodeResult result(int index);

        /** Cancel all still-pending children. */
        void cancelAll(String reason);
    }
}
