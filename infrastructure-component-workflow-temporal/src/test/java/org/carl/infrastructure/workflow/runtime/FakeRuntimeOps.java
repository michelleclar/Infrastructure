package org.carl.infrastructure.workflow.runtime;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link RuntimeOps} for {@link WorkflowInterpreter} unit tests — no Temporal runtime.
 *
 * <p>Activities return scripted (or default-success) results; awaited events are taken from a
 * pre-loaded queue (or report a timeout when one is requested). This lets the full orchestration
 * loop run on a plain JVM.
 */
final class FakeRuntimeOps implements RuntimeOps {

    private final Map<String, ActivityResult> activityResults = new HashMap<>();
    private final Deque<WorkflowEvent> pendingEvents = new ArrayDeque<>();
    private final Map<String, WorkflowResult> subResults = new HashMap<>();

    // Observability for assertions.
    final List<String> activityCalls = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    int archiveCalls = 0;
    int asyncHookCalls = 0;
    int sleeps = 0;

    FakeRuntimeOps onActivity(String name, ActivityResult result) {
        activityResults.put(name, result);
        return this;
    }

    FakeRuntimeOps offerEvent(WorkflowEvent event) {
        pendingEvents.add(event);
        return this;
    }

    FakeRuntimeOps onSubProcess(String definitionId, WorkflowResult result) {
        subResults.put(definitionId, result);
        return this;
    }

    @Override
    public ActivityResult runActivity(String activityName, Map<String, Object> input) {
        activityCalls.add(activityName);
        return activityResults.getOrDefault(activityName, new ActivityResult(true, Map.of(), null));
    }

    @Override
    public void sleep(Duration duration) {
        sleeps++;
    }

    @Override
    public AwaitOutcome awaitEvent(EventMatcher matcher, Duration timeout) {
        Iterator<WorkflowEvent> it = pendingEvents.iterator();
        while (it.hasNext()) {
            WorkflowEvent ev = it.next();
            if (matcher.matches(ev)) {
                it.remove();
                return new AwaitOutcome(ev, false);
            }
        }
        if (timeout != null) {
            return new AwaitOutcome(null, true);
        }
        throw new IllegalStateException(
                "FakeRuntimeOps.awaitEvent: no matching event queued and no timeout — the"
                        + " interpreter would block forever. Offer an event or set a timeout.");
    }

    @Override
    public WorkflowResult runSubProcess(
            WorkflowDefinition childDef, Map<String, Object> input, String idHint) {
        WorkflowResult r = subResults.get(childDef.id());
        if (r == null) {
            throw new IllegalStateException("no sub-process result scripted for " + childDef.id());
        }
        return r;
    }

    @Override
    public NodeResult runTaskGroup(
            Map<String, Object> intentPayload,
            NodeDefinition parentNode,
            String parentQualifier,
            TaskGroupChildExecutor childExecutor,
            TaskGroupJoiner joiner) {
        throw new UnsupportedOperationException(
                "taskGroup is not exercised by interpreter unit tests (covered end-to-end on"
                        + " Temporal)");
    }

    @Override
    public void emitAsyncHook(AsyncHookInvocation invocation) {
        asyncHookCalls++;
    }

    @Override
    public void awaitAsyncHooks() {
        // no-op
    }

    @Override
    public void archive(WorkflowResult result) {
        archiveCalls++;
    }

    @Override
    public void warn(String message) {
        warnings.add(message);
    }
}
