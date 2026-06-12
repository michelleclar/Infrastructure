package org.carl.infrastructure.workflow.handlers;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.handlers.TaskGroupConfig.JoinRule;
import org.carl.infrastructure.workflow.handlers.TaskGroupConfig.TaskGroupChild;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Built-in handler for {@code taskGroup} nodes.
 *
 * <p>The handler does not run its children; it asks the runtime to do so via {@link
 * RuntimeIntents}. The runtime stores each child's {@link NodeResult} keyed by {@link
 * TaskGroupContract#childKey(String, String)} and notifies the parent group via internal {@value
 * #CHILD_COMPLETED_EVENT} events. On each notification the parent re-evaluates its own outcome
 * using {@link #aggregate(JoinRule, List)}.
 */
public final class TaskGroupHandler implements NodeHandler<TaskGroupConfig, Object, Object> {

    /** Internal event the runtime emits whenever a child task finishes. */
    public static final String CHILD_COMPLETED_EVENT = "_childCompleted";

    @Override
    public String type() {
        return NodeTypes.TASK_GROUP;
    }

    @Override
    public Class<TaskGroupConfig> configType() {
        return TaskGroupConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(
                Outcomes.APPROVED,
                Outcomes.REJECTED,
                Outcomes.SENDBACK,
                Outcomes.COMPLETED,
                Outcomes.FAILED,
                Outcomes.TIMEOUT,
                Outcomes.CANCELLED);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, TaskGroupConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        JoinRule rule = effectiveJoinRule(config);
        payload.put(RuntimeIntents.JOIN_RULE, rule.wireName());

        List<Map<String, Object>> serialized = new ArrayList<>();
        if (config != null && config.tasks() != null) {
            for (TaskGroupChild child : config.tasks()) {
                if (child == null) {
                    continue;
                }
                Map<String, Object> dto = new LinkedHashMap<>();
                if (child.id() != null) dto.put("id", child.id());
                if (child.label() != null) dto.put("label", child.label());
                if (child.type() != null) dto.put("type", child.type());
                if (child.config() != null) dto.put("config", child.config());
                serialized.add(dto);
            }
        }
        payload.put(RuntimeIntents.CHILDREN, serialized);
        return new NodeResult(NodeStatus.WAITING, null, payload, null);
    }

    @Override
    public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent ev, TaskGroupConfig cfg) {
        return ev != null && CHILD_COMPLETED_EVENT.equals(ev.name());
    }

    @Override
    public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent ev, TaskGroupConfig cfg) {
        if (ev == null || !CHILD_COMPLETED_EVENT.equals(ev.name())) {
            return NodeResult.waiting();
        }
        if (cfg == null || cfg.tasks() == null || cfg.tasks().isEmpty()) {
            // No declared children: nothing to wait for; treat as immediate APPROVED.
            return NodeResult.completed(Outcomes.APPROVED);
        }
        JoinRule rule = effectiveJoinRule(cfg);
        List<NodeResult> childResults = new ArrayList<>();
        String parent = ctx == null ? null : ctx.currentNodeId();
        for (TaskGroupChild child : cfg.tasks()) {
            if (child == null || child.id() == null || child.id().isBlank()) {
                continue;
            }
            NodeResult r = null;
            // parent != null already implies ctx != null (parent is derived from ctx above).
            if (parent != null) {
                r = ctx.resultOf(TaskGroupContract.childKey(parent, child.id()));
            }
            childResults.add(r);
        }
        return aggregate(rule, childResults);
    }

    /**
     * Pure aggregation logic over a list of child results (some entries may be {@code null} meaning
     * the child has not completed yet). Exposed package-private for unit testing.
     */
    static NodeResult aggregate(JoinRule rule, List<NodeResult> children) {
        if (children == null || children.isEmpty()) {
            return NodeResult.completed(Outcomes.APPROVED);
        }
        if (rule == JoinRule.ALL) {
            // Under ALL, a single REJECTED child immediately fails the group — there is no
            // point waiting for the remaining children because they cannot change the outcome.
            // REJECTED is checked before SENDBACK so that a mix of reject+sendback resolves to
            // the stricter result (rejection), matching typical approval-process semantics.
            for (NodeResult r : children) {
                if (r == null) continue;
                if (r.status() == NodeStatus.COMPLETED && Outcomes.REJECTED.equals(r.outcome())) {
                    return NodeResult.completed(Outcomes.REJECTED);
                }
            }
            // SENDBACK short-circuits after REJECTED: one reviewer asking to revise supersedes
            // all pending approvals and causes the whole group to send back.
            for (NodeResult r : children) {
                if (r == null) continue;
                if (r.status() == NodeStatus.COMPLETED && Outcomes.SENDBACK.equals(r.outcome())) {
                    return NodeResult.completed(Outcomes.SENDBACK);
                }
            }
            // No early-exit outcome: require every child to be complete and approved.
            // A null entry means the runtime has not yet delivered a result for that child.
            for (NodeResult r : children) {
                if (r == null) {
                    return NodeResult.waiting();
                }
                if (r.status() != NodeStatus.COMPLETED || !isApprovedLike(r.outcome())) {
                    return NodeResult.waiting();
                }
            }
            return NodeResult.completed(Outcomes.APPROVED);
        }
        // JoinRule.ANY
        boolean sawWaiting = false;
        for (NodeResult r : children) {
            if (r == null) {
                // Null means the runtime has not recorded this child yet. Under ANY, an unfinished
                // child keeps the group waiting unless another child already approved.
                sawWaiting = true;
                continue;
            }
            if (r.status() == NodeStatus.COMPLETED && isApprovedLike(r.outcome())) {
                return NodeResult.completed(Outcomes.APPROVED);
            }
        }
        if (sawWaiting) {
            // At least one child is still running and none approved yet — keep waiting.
            return NodeResult.waiting();
        }
        // All children have finished and none produced an approved-like outcome: the group
        // is collectively rejected (every child either rejected or sent back).
        return NodeResult.completed(Outcomes.REJECTED);
    }

    /**
     * Returns {@code true} for outcomes that count as "positive" for join purposes.
     *
     * <p>The three aliases reflect the fact that different built-in task types use different outcome
     * names for their happy path ({@code approved} for approval tasks, {@code success} for service
     * tasks, {@code completed} for user tasks). Treating all three as equivalent lets a mixed-type
     * taskGroup evaluate correctly without special-casing each child type.
     */
    private static boolean isApprovedLike(String outcome) {
        return Outcomes.APPROVED.equals(outcome)
                || Outcomes.SUCCESS.equals(outcome)
                || Outcomes.COMPLETED.equals(outcome);
    }

    private static JoinRule effectiveJoinRule(TaskGroupConfig cfg) {
        if (cfg != null && cfg.join() != null) {
            return cfg.join();
        }
        return JoinRule.ALL;
    }
}
