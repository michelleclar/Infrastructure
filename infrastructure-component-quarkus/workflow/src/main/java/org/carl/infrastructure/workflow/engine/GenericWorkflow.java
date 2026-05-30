package org.carl.infrastructure.workflow.engine;

import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.DynamicQueryHandler;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import org.carl.infrastructure.workflow.api.ProcessDefinition;
import org.carl.infrastructure.workflow.api.ProcessFlow;
import org.carl.infrastructure.workflow.api.ProcessRegistry;
import org.carl.infrastructure.workflow.api.RetryPolicy;
import org.carl.infrastructure.workflow.api.WorkflowActivity;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * The single generic Temporal workflow that interprets any registered {@link ProcessDefinition}.
 * It is the only workflow type registered on the worker; business never writes a
 * {@code @WorkflowInterface}.
 *
 * <p>Loop: wait for the next event (signal, or a timeout-mapped event) → ask the compiled
 * {@link ProcessFlow} to resolve the next state (deterministic decision) → run that transition's
 * {@link WorkflowActivity} (if any) as a Temporal activity (timeout/retry per the activity's
 * pure-function declarations) → advance. On failure, run registered Saga compensations in reverse,
 * each carrying the transition (from/to/event/ctx) captured when its forward step ran.
 *
 * <p><b>Determinism contract:</b>
 * <ul>
 *   <li>{@link ProcessFlow#resolve} (guard evaluation) runs in workflow code — guards MUST be pure.
 *   <li>{@link WorkflowActivity#timeout} and {@link WorkflowActivity#retry} are called in workflow
 *       code — they MUST be pure functions of ctx (no IO, clock, or randomness).
 *   <li>{@link WorkflowActivity#run} and {@link WorkflowActivity#compensate} execute as Temporal
 *       activities — IO is allowed.
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GenericWorkflow implements DynamicWorkflow {

    private Object currentState;

    @Override
    public Object execute(EncodedValues args) {
        String processId = Workflow.getInfo().getWorkflowType();
        ProcessDefinition def = ProcessRegistry.definition(processId);
        ProcessFlow flow = ProcessRegistry.flow(processId);
        Object ctx = args.get(0, def.ctxType());

        Deque<Object> inbox = new ArrayDeque<>();
        Workflow.registerListener(
                (DynamicSignalHandler)
                        (signalName, signalArgs) -> inbox.add(signalArgs.get(0, def.eventType())));
        currentState = def.startState();
        Workflow.registerListener((DynamicQueryHandler) (queryType, queryArgs) -> currentState);

        Saga saga = new Saga(new Saga.Options.Builder().build());
        try {
            while (!def.isTerminal(currentState)) {
                Object event = awaitNextEvent(def, currentState, ctx, inbox);
                if (event == null) {
                    break; // finite await elapsed and no timeout transition defined
                }
                Object from = currentState;

                // Resolve the next state + bound activity. Guards run here (workflow code — pure).
                ProcessFlow.ResolvedTransition resolved = flow.resolve(from, event, ctx);
                if (resolved == null) {
                    continue; // no matching transition / guard failed → keep waiting
                }

                Object next = resolved.to();
                if (Objects.equals(next, from)) {
                    continue; // self-transition → keep waiting
                }

                // Run the transition's activity (if any) before advancing state
                runActivity(processId, resolved.activity(), from, next, event, ctx, saga);
                currentState = next;
            }
        } catch (Exception e) {
            saga.compensate();
            throw e;
        }
        return currentState;
    }

    private Object awaitNextEvent(
            ProcessDefinition def, Object state, Object ctx, Deque<Object> inbox) {
        Duration wait = def.awaitTimeout(state, ctx);
        if (wait == null) {
            Workflow.await(() -> !inbox.isEmpty());
            return inbox.poll();
        }
        boolean signalled = Workflow.await(wait, () -> !inbox.isEmpty());
        return signalled ? inbox.poll() : def.onTimeout(state);
    }

    /**
     * Schedules the given activity as a Temporal activity, then registers a Saga compensation if
     * the activity is compensable. The compensation uses the SAME timeout/retry as the forward call
     * (fix: no more hardcoded defaults for compensation options).
     *
     * @param activity may be {@code null} (routing-only transition) — returns immediately
     */
    private void runActivity(
            String processId,
            WorkflowActivity activity,
            Object from,
            Object to,
            Object event,
            Object ctx,
            Saga saga) {
        if (activity == null) {
            return; // routing-only transition, nothing to run
        }

        // timeout and retry are pure functions of ctx; safe to evaluate here (workflow code)
        Duration timeout = activity.timeout(ctx);
        RetryPolicy retry = activity.retry(ctx);

        // Namespaced Temporal activity-type name: processId + "::" + activity.name()
        String activityType = ProcessRegistry.namespacedName(processId, activity.name());

        ActivityStub stub =
                Workflow.newUntypedActivityStub(TemporalMapper.activityOptions(timeout, retry));
        // pass the full transition so the activity sees from/to/event/ctx
        stub.execute(activityType, Void.class, from, to, event, ctx);

        if (activity.compensable()) {
            String compensationType = activityType + ProcessRegistry.COMPENSATE_SUFFIX;
            // Compensation uses the SAME timeout/retry as the forward activity
            // (transition-declared options, not a hardcoded fallback)
            ActivityStub compensationStub =
                    Workflow.newUntypedActivityStub(TemporalMapper.activityOptions(timeout, retry));
            // Registered after the forward step succeeds: only compensate completed work.
            // The compensation receives the same from/to/event/ctx captured at forward run time.
            saga.addCompensation(
                    () ->
                            compensationStub.execute(
                                    compensationType, Void.class, from, to, event, ctx));
        }
    }
}
