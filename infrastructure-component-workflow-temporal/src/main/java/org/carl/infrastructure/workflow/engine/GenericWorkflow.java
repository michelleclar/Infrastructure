package org.carl.infrastructure.workflow.engine;

import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.DynamicQueryHandler;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import org.carl.infrastructure.workflow.api.ApprovalConfig;
import org.carl.infrastructure.workflow.api.Decision;
import org.carl.infrastructure.workflow.api.Expr;
import org.carl.infrastructure.workflow.api.ExprEvaluator;
import org.carl.infrastructure.workflow.api.ProcessDefinition;
import org.carl.infrastructure.workflow.api.ProcessFlow;
import org.carl.infrastructure.workflow.api.ProcessRegistry;
import org.carl.infrastructure.workflow.api.RetryPolicy;
import org.carl.infrastructure.workflow.api.Step;
import org.carl.infrastructure.workflow.api.Tri;
import org.carl.infrastructure.workflow.api.Vote;
import org.carl.infrastructure.workflow.api.WorkflowActivity;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The single generic Temporal workflow that interprets any registered {@link ProcessDefinition}.
 * It is the only workflow type registered on the worker; business never writes a
 * {@code @WorkflowInterface}.
 *
 * <p>Loop: on entering a state, EITHER advance via a single {@code event} signal (plain transition),
 * OR — if the state is declared as an approval state — collect {@code vote} signals and evaluate a
 * boolean expression tree ({@link Expr}) to decide APPROVED/REJECTED. Each step's bound
 * {@link WorkflowActivity} runs as a Temporal activity with its own timeout/retry; failures
 * trigger Saga compensations registered after successful forward calls.
 *
 * <p><b>Determinism contract:</b>
 *
 * <ul>
 *   <li>{@link ProcessFlow#resolve} (guard evaluation) runs in workflow code — guards MUST be pure.
 *   <li>{@link ExprEvaluator#evaluate} runs in workflow code — MUST be pure.
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

    /** Pending vote signals for the CURRENT approval state (one vote dequeued per iteration). */
    private final Deque<Vote> pendingVotes = new ArrayDeque<>();

    /**
     * Replay-aware logger. Initialized once in {@link #execute} via {@code Workflow.getLogger};
     * shared across all workflow-thread methods so we don't allocate per call.
     */
    private Logger log;

    @Override
    public Object execute(EncodedValues args) {
        // Initialize the replay-aware logger. Temporal's Workflow.getLogger suppresses
        // duplicate emissions during history replay, keeping test output clean.
        log = Workflow.getLogger(GenericWorkflow.class);

        String processId = Workflow.getInfo().getWorkflowType();
        ProcessDefinition def = ProcessRegistry.definition(processId);
        ProcessFlow flow = ProcessRegistry.flow(processId);
        Object ctx = args.get(0, def.ctxType());

        // Two signal types: "event" advances a normal transition; "vote" feeds the approval loop.
        // Both registered up-front so signals delivered before we await them are queued by Temporal.
        Workflow.registerListener(
                (DynamicSignalHandler)
                        (signalName, signalArgs) -> {
                            if ("vote".equals(signalName)) {
                                Vote v = signalArgs.get(0, Vote.class);
                                if (v == null || v.getStep() == null) {
                                    log.warn("dropping invalid vote signal (null step): {}", v);
                                    return;
                                }
                                log.debug(
                                        "signal vote step={} approver={} decision={} pending={}",
                                        v.getStep(),
                                        v.getApprover(),
                                        v.getDecision(),
                                        pendingVotes.size() + 1);
                                pendingVotes.add(v);
                            } else {
                                Object event = signalArgs.get(0, def.eventType());
                                log.debug(
                                        "signal event={} pending={}",
                                        event,
                                        eventInbox.size() + 1);
                                eventInbox.add(event);
                            }
                        });
        currentState = def.startState();
        Workflow.registerListener((DynamicQueryHandler) (queryType, queryArgs) -> currentState);

        log.debug(
                "workflow start processId={} workflowId={} runId={} startState={}",
                processId,
                Workflow.getInfo().getWorkflowId(),
                Workflow.getInfo().getRunId(),
                currentState);

        Saga saga = new Saga(new Saga.Options.Builder().build());
        try {
            while (!def.isTerminal(currentState)) {
                ApprovalConfig approval = ProcessRegistry.approval(processId, currentState);
                if (approval != null) {
                    log.debug("entering state={} approval=true", currentState);
                    // Approval state: evaluate expression tree until TRUE or FALSE
                    advanceViaApproval(processId, approval, ctx, saga);
                } else {
                    log.debug("entering state={} approval=false", currentState);
                    // Plain transition: wait for one event signal (or a timeout-mapped event)
                    if (!advanceViaEvent(processId, def, flow, ctx, saga)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("workflow {} failed: {}; running saga compensation",
                    Workflow.getInfo().getWorkflowId(), e.getMessage());
            saga.compensate();
            throw e;
        }

        if (def.isTerminal(currentState)) {
            log.info("workflow {} terminal state={}", Workflow.getInfo().getWorkflowId(), currentState);
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
        log.debug("awaiting event in state={}", currentState);
        Object event = awaitNextEvent(def, currentState, ctx);
        if (event == null) {
            return false;
        }
        Object from = currentState;

        ProcessFlow.ResolvedTransition resolved = flow.resolve(from, event, ctx);
        if (resolved == null) {
            log.debug("no transition for state={} event={} (or guard failed)", from, event);
            return true; // no match / guard failed → keep waiting
        }
        Object next = resolved.to();
        if (Objects.equals(next, from)) {
            log.debug("self-transition state={} event={}", from, event);
            return true; // self-transition → keep waiting
        }
        String activityName = resolved.activity() != null ? resolved.activity().name() : "(none)";
        log.debug("transition {} --{}--> {} activity={}", from, event, next, activityName);
        runActivity(processId, resolved.activity(), from, next, event, ctx, null, saga);
        currentState = next;
        return true;
    }

    /**
     * Expression-based approval (会签 v2) path.
     *
     * <ol>
     *   <li>Collect all leaf {@link Step}s from the expression tree.
     *   <li>Fire all {@code before} hooks sequentially (pre-order tree walk).
     *   <li>Loop: await the next vote, process it (update vote map or clear on SENDBACK), fire
     *       {@code after} hook, detect UNKNOWN→terminal leaf transitions for {@code onComplete}
     *       hooks, re-evaluate the whole expression.
     *   <li>On TRUE → run approvedActivity + advance; on FALSE → run rejectedActivity + advance.
     * </ol>
     */
    private void advanceViaApproval(
            String processId,
            ApprovalConfig approval,
            Object ctx,
            Saga saga) {
        Object from = currentState;
        Expr expr = approval.expr();

        // Collect all step names (valid targets for votes)
        Set<String> leafNames = new HashSet<>();
        List<Step> steps = new ArrayList<>();
        collectSteps(expr, steps);
        for (Step s : steps) {
            leafNames.add(s.name());
        }

        // Count hooks for the init log
        int beforeCount = 0, afterCount = 0, onCompleteCount = 0;
        for (Step s : steps) {
            if (s.beforeHook() != null) beforeCount++;
            if (s.afterHook() != null) afterCount++;
            if (s.onCompleteHook() != null) onCompleteCount++;
        }
        log.debug(
                "approval state={} leaves={} hooks={{before:{},after:{},onComplete:{}}}",
                from, steps.size(), beforeCount, afterCount, onCompleteCount);

        // Maps for tracking votes and detecting onComplete transitions
        Map<String, Decision> votes = new HashMap<>();
        Map<String, Tri> previousLeafStates = new HashMap<>();
        for (Step s : steps) {
            previousLeafStates.put(s.name(), Tri.UNKNOWN);
        }

        // Fire all before hooks (sequential, pre-order)
        for (Step s : steps) {
            if (s.beforeHook() != null) {
                String hookActivityType = ProcessRegistry.namespacedName(
                        processId,
                        ProcessRegistry.hookActivityName(s.name(), "before"));
                log.debug("firing before hook step={} activity={}", s.name(), hookActivityType);
                runHookActivity(processId, s.beforeHook(), hookActivityType, from, null, null, ctx, s.name(), saga);
            }
        }

        // Vote-evaluation loop
        while (true) {
            // Wait until there is a pending vote
            Workflow.await(() -> !pendingVotes.isEmpty());
            Vote v = pendingVotes.poll();

            if (v == null) {
                continue; // shouldn't happen but guard defensively
            }

            // Ignore votes for steps not in this expression
            if (!leafNames.contains(v.getStep())) {
                log.warn(
                        "approval state [{}]: dropping vote for unknown step [{}]",
                        from,
                        v.getStep());
                continue;
            }

            String stepName = v.getStep();

            // Apply vote or clear on SENDBACK
            if (v.getDecision() == Decision.SENDBACK) {
                votes.remove(stepName);
            } else {
                votes.put(stepName, v.getDecision());
            }
            log.debug("vote applied step={} decision={} votes={}", stepName, v.getDecision(), votes);

            // Fire after hook for this step (if defined)
            Step stepObj = findStep(steps, stepName);
            if (stepObj != null && stepObj.afterHook() != null) {
                String hookActivityType = ProcessRegistry.namespacedName(
                        processId,
                        ProcessRegistry.hookActivityName(stepName, "after"));
                log.debug("firing after hook step={} activity={}", stepName, hookActivityType);
                runHookActivity(processId, stepObj.afterHook(), hookActivityType, from, null, null, ctx, stepName, saga);
            }

            // Compute new leaf states and fire onComplete for UNKNOWN→terminal transitions
            for (Step s : steps) {
                Tri prev = previousLeafStates.get(s.name());
                Tri curr = ExprEvaluator.evalLeaf(votes.get(s.name()));

                // On SENDBACK, the entry was removed from votes so evalLeaf returns UNKNOWN.
                // Update previousLeafStates to reflect the current state.
                previousLeafStates.put(s.name(), curr);

                if (prev == Tri.UNKNOWN && curr != Tri.UNKNOWN) {
                    // Transitioned from UNKNOWN to a terminal value — fire onComplete
                    if (s.onCompleteHook() != null) {
                        String hookActivityType = ProcessRegistry.namespacedName(
                                processId,
                                ProcessRegistry.hookActivityName(s.name(), "onComplete"));
                        log.debug("leaf {} UNKNOWN→{}, firing onComplete activity={}",
                                s.name(), curr, hookActivityType);
                        runHookActivity(processId, s.onCompleteHook(), hookActivityType, from, null, null, ctx, s.name(), saga);
                    }
                }
            }

            // Re-evaluate the overall expression
            Tri result = ExprEvaluator.evaluate(expr, votes);
            log.debug("evaluator result={} votes={}", result, votes);

            if (result == Tri.TRUE) {
                // Approved outcome
                pendingVotes.clear(); // discard late-arriving votes
                Object next = approval.approvedTo();
                WorkflowActivity approvedActivity = approval.approvedActivity();
                log.info("approval state={} → APPROVED advancing to={}", from, next);
                runActivity(processId, approvedActivity, from, next, null, ctx, null, saga);
                currentState = next;
                return;
            } else if (result == Tri.FALSE) {
                // Rejected outcome
                pendingVotes.clear();
                Object next = approval.rejectedTo();
                WorkflowActivity rejectedActivity = approval.rejectedActivity();
                log.info("approval state={} → REJECTED advancing to={}", from, next);
                runActivity(processId, rejectedActivity, from, next, null, ctx, null, saga);
                currentState = next;
                return;
            }
            // UNKNOWN → continue loop
        }
    }

    /** Find the Step with the given name in the pre-collected list. Pure (no IO). */
    private static Step findStep(List<Step> steps, String name) {
        for (Step s : steps) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }

    /** Recursively collect all Step leaf nodes (pre-order). Pure (no IO). */
    private static void collectSteps(Expr expr, List<Step> out) {
        if (expr instanceof Step s) {
            out.add(s);
        } else if (expr instanceof org.carl.infrastructure.workflow.api.And and) {
            for (Expr child : and.children()) collectSteps(child, out);
        } else if (expr instanceof org.carl.infrastructure.workflow.api.Or or) {
            for (Expr child : or.children()) collectSteps(child, out);
        } else if (expr instanceof org.carl.infrastructure.workflow.api.AtLeast al) {
            for (Expr child : al.children()) collectSteps(child, out);
        }
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
     * Run a hook activity without Saga compensation registration. Hooks are fire-and-forget
     * side-effects; if they fail the workflow fails (and the outer Saga compensates everything that
     * ran before). Step is passed as the 5th arg.
     */
    private void runHookActivity(
            String processId,
            WorkflowActivity activity,
            String activityType,
            Object from,
            Object to,
            Object event,
            Object ctx,
            String step,
            Saga saga) {
        if (activity == null) {
            return;
        }
        Duration timeout = activity.timeout(ctx);
        RetryPolicy retry = activity.retry(ctx);
        int retryMax = retry != null ? retry.maxAttempts() : 0;
        log.debug("scheduling activity={} timeout={} retry-max={}", activityType, timeout, retryMax);
        ActivityStub stub =
                Workflow.newUntypedActivityStub(TemporalMapper.activityOptions(timeout, retry));
        stub.execute(activityType, Void.class, from, to, event, ctx, step);
        // Hooks do not participate in Saga (no compensation registration for hooks in v1)
    }

    /**
     * Schedules the given activity as a Temporal activity, then registers a Saga compensation if
     * the activity is compensable. The compensation uses the SAME timeout/retry as the forward call.
     *
     * @param activity may be {@code null} (routing-only or no outcome activity) — returns immediately
     * @param step     the hook step name, or null for non-hook activities
     */
    private void runActivity(
            String processId,
            WorkflowActivity activity,
            Object from,
            Object to,
            Object event,
            Object ctx,
            String step,
            Saga saga) {
        if (activity == null) {
            return;
        }
        Duration timeout = activity.timeout(ctx);
        RetryPolicy retry = activity.retry(ctx);
        String activityType = ProcessRegistry.namespacedName(processId, activity.name());
        int retryMax = retry != null ? retry.maxAttempts() : 0;
        log.debug("scheduling activity={} timeout={} retry-max={}", activityType, timeout, retryMax);

        ActivityStub stub =
                Workflow.newUntypedActivityStub(TemporalMapper.activityOptions(timeout, retry));
        stub.execute(activityType, Void.class, from, to, event, ctx, step);

        if (activity.compensable()) {
            String compensationType = activityType + ProcessRegistry.COMPENSATE_SUFFIX;
            log.debug("registered saga compensation for activity={}", compensationType);
            ActivityStub compensationStub =
                    Workflow.newUntypedActivityStub(TemporalMapper.activityOptions(timeout, retry));
            saga.addCompensation(
                    () ->
                            compensationStub.execute(
                                    compensationType, Void.class, from, to, event, ctx, step));
        }
    }
}
