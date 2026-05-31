package org.carl.infrastructure.workflow.engine;

import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.DynamicQueryHandler;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import org.carl.infrastructure.workflow.api.ApprovalPolicy;
import org.carl.infrastructure.workflow.api.GatheringConfig;
import org.carl.infrastructure.workflow.api.ProcessDefinition;
import org.carl.infrastructure.workflow.api.ProcessFlow;
import org.carl.infrastructure.workflow.api.ProcessRegistry;
import org.carl.infrastructure.workflow.api.RetryPolicy;
import org.carl.infrastructure.workflow.api.Vote;
import org.carl.infrastructure.workflow.api.WorkflowActivity;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The single generic Temporal workflow that interprets any registered {@link ProcessDefinition}.
 * It is the only workflow type registered on the worker; business never writes a
 * {@code @WorkflowInterface}.
 *
 * <p>Loop: on entering a state, EITHER advance via a single {@code event} signal (plain transition),
 * OR — if the state is declared as a gathering state — collect {@code vote} signals from N
 * assignees and let an {@link ApprovalPolicy} decide APPROVED/REJECTED. Each step's bound
 * {@link WorkflowActivity} runs as a Temporal activity with its own timeout/retry; failures
 * trigger Saga compensations registered after successful forward calls.
 *
 * <p><b>Determinism contract:</b>
 * <ul>
 *   <li>{@link ProcessFlow#resolve} (guard evaluation) runs in workflow code — guards MUST be pure.
 *   <li>{@link ApprovalPolicy#evaluate} runs in workflow code — MUST be pure.
 *   <li>{@link GatheringConfig#assignees()} is called once at state entry — MUST be pure.
 *   <li>{@link WorkflowActivity#timeout} and {@link WorkflowActivity#retry} run in workflow code
 *       — MUST be pure functions of ctx.
 *   <li>{@link WorkflowActivity#run} and {@link WorkflowActivity#compensate} execute as Temporal
 *       activities — IO is allowed.
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GenericWorkflow implements DynamicWorkflow {

    private Object currentState;

    /** Inbox for plain "event" signals (single-event transition path). */
    private final Deque<Object> eventInbox = new ArrayDeque<>();

    /** Latest vote per approver for the CURRENT gathering state (cleared on state exit). */
    private final Map<String, Vote> currentVotes = new HashMap<>();

    @Override
    public Object execute(EncodedValues args) {
        String processId = Workflow.getInfo().getWorkflowType();
        ProcessDefinition def = ProcessRegistry.definition(processId);
        ProcessFlow flow = ProcessRegistry.flow(processId);
        Object ctx = args.get(0, def.ctxType());

        Logger log = Workflow.getLogger(GenericWorkflow.class);
        // Two signal types: "event" advances a normal transition; "vote" feeds the gather loop.
        // Both registered up-front so signals delivered before we await them are queued by Temporal.
        Workflow.registerListener(
                (DynamicSignalHandler)
                        (signalName, signalArgs) -> {
                            if ("vote".equals(signalName)) {
                                Vote v = signalArgs.get(0, Vote.class);
                                if (v == null || v.getApprover() == null) {
                                    log.warn("dropping invalid vote signal: {}", v);
                                    return;
                                }
                                // Non-assignee filter happens in the gather loop where we have
                                // the assignee set; here we just record by approver id.
                                currentVotes.put(v.getApprover(), v);
                            } else {
                                eventInbox.add(signalArgs.get(0, def.eventType()));
                            }
                        });
        currentState = def.startState();
        Workflow.registerListener((DynamicQueryHandler) (queryType, queryArgs) -> currentState);

        Saga saga = new Saga(new Saga.Options.Builder().build());
        try {
            while (!def.isTerminal(currentState)) {
                GatheringConfig gathering = ProcessRegistry.gathering(processId, currentState);
                if (gathering != null) {
                    // Gathering state: collect votes until policy reaches a terminal outcome
                    advanceViaGathering(processId, gathering, ctx, saga);
                } else {
                    // Plain transition: wait for one event signal (or a timeout-mapped event)
                    if (!advanceViaEvent(processId, def, flow, ctx, saga)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            saga.compensate();
            throw e;
        }
        return currentState;
    }

    /**
     * Plain (single-event) transition path. Returns {@code true} if the loop should continue, or
     * {@code false} if the finite await elapsed and no timeout transition is defined (end of run).
     */
    private boolean advanceViaEvent(
            String processId,
            ProcessDefinition def,
            ProcessFlow flow,
            Object ctx,
            Saga saga) {
        Object event = awaitNextEvent(def, currentState, ctx);
        if (event == null) {
            return false;
        }
        Object from = currentState;

        ProcessFlow.ResolvedTransition resolved = flow.resolve(from, event, ctx);
        if (resolved == null) {
            return true; // no match / guard failed → keep waiting
        }
        Object next = resolved.to();
        if (Objects.equals(next, from)) {
            return true; // self-transition → keep waiting
        }
        runActivity(processId, resolved.activity(), from, next, event, ctx, saga);
        currentState = next;
        return true;
    }

    /**
     * Gathering (会签) path. Computes assignees once at state entry, then awaits {@code vote}
     * signals until {@link ApprovalPolicy} returns a terminal outcome. Non-assignee votes are
     * filtered out; late votes (arriving after the outcome is decided) are simply ignored because
     * the loop exits and {@link #currentVotes} is cleared on state exit.
     */
    private void advanceViaGathering(
            String processId, GatheringConfig gathering, Object ctx, Saga saga) {
        Object from = currentState;

        // Snapshot the assignee set at entry (pure function of ctx)
        Collection<String> rawAssignees = (Collection<String>) gathering.assignees().apply(ctx);
        if (rawAssignees == null || rawAssignees.isEmpty()) {
            throw new IllegalStateException(
                    "Gathering state ["
                            + from
                            + "] resolved to an empty assignee set; cannot proceed.");
        }
        Set<String> assignees = new LinkedHashSet<>(rawAssignees);

        // Filter currentVotes to assignees only (drop any votes from non-assignees already queued)
        currentVotes.keySet().retainAll(assignees);

        ApprovalPolicy policy = gathering.policy();
        // Block until policy decides
        Workflow.await(
                () -> {
                    // Drop any non-assignee votes that arrived during the wait (defensive)
                    currentVotes.keySet().retainAll(assignees);
                    return policy.evaluate(assignees, currentVotes) != ApprovalPolicy.Outcome.WAIT;
                });
        ApprovalPolicy.Outcome outcome = policy.evaluate(assignees, currentVotes);

        Object next;
        WorkflowActivity activity;
        if (outcome == ApprovalPolicy.Outcome.APPROVED) {
            next = gathering.approvedTo();
            activity = gathering.approvedActivity();
        } else {
            next = gathering.rejectedTo();
            activity = gathering.rejectedActivity();
        }

        // Reset vote inbox before advancing — votes from one gathering state don't leak to next
        currentVotes.clear();

        // No business event triggered the transition (the outcome did). Pass null so the activity
        // receives NodeContext.event() == null (consistent with start-state entries). The activity
        // can read `to` to know whether it's the APPROVED or REJECTED branch.
        runActivity(processId, activity, from, next, null, ctx, saga);
        currentState = next;
    }

    private Object awaitNextEvent(ProcessDefinition def, Object state, Object ctx) {
        Duration wait = def.awaitTimeout(state, ctx);
        if (wait == null) {
            Workflow.await(() -> !eventInbox.isEmpty());
            return eventInbox.poll();
        }
        boolean signalled = Workflow.await(wait, () -> !eventInbox.isEmpty());
        return signalled ? eventInbox.poll() : def.onTimeout(state);
    }

    /**
     * Schedules the given activity as a Temporal activity, then registers a Saga compensation if
     * the activity is compensable. The compensation uses the SAME timeout/retry as the forward call.
     *
     * @param activity may be {@code null} (routing-only or no outcome activity) — returns immediately
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
            return;
        }
        Duration timeout = activity.timeout(ctx);
        RetryPolicy retry = activity.retry(ctx);
        String activityType = ProcessRegistry.namespacedName(processId, activity.name());

        ActivityStub stub =
                Workflow.newUntypedActivityStub(TemporalMapper.activityOptions(timeout, retry));
        stub.execute(activityType, Void.class, from, to, event, ctx);

        if (activity.compensable()) {
            String compensationType = activityType + ProcessRegistry.COMPENSATE_SUFFIX;
            ActivityStub compensationStub =
                    Workflow.newUntypedActivityStub(TemporalMapper.activityOptions(timeout, retry));
            saga.addCompensation(
                    () ->
                            compensationStub.execute(
                                    compensationType, Void.class, from, to, event, ctx));
        }
    }
}
