package org.carl.infrastructure.workflow.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry of processes and their compiled {@link ProcessFlow}s. Intentionally static:
 * Temporal instantiates {@link org.carl.infrastructure.workflow.engine.GenericWorkflow} outside
 * CDI, so the engine reaches definitions via this singleton. Wired from CDI at startup (see runtime
 * bootstrap) or directly in tests.
 *
 * <h3>Activity-name namespacing</h3>
 *
 * <p>Activity names are SCOPED to a process. The Temporal activity-type name used on the wire is:
 *
 * <pre>
 *   forward call:      {@code processId + "::" + activity.name()}
 *   compensation call: {@code processId + "::" + activity.name() + "#compensate"}
 * </pre>
 *
 * Hook activity names are further namespaced with the step and hook type:
 *
 * <pre>
 *   {@code processId + "::" + stepName + ":before|after|onComplete"}
 * </pre>
 *
 * Constants: {@link #PROCESS_SEP} ({@code "::"}) and {@link #COMPENSATE_SUFFIX} ({@code
 * "#compensate"}).
 *
 * <h3>Duplicate detection</h3>
 *
 * <p>Within a single process, {@link WorkflowActivity#name()} must be unique. A clear error is
 * thrown at registration time if the same name appears in two different transitions or hooks.
 */
public final class ProcessRegistry {

    /** Separator between processId and activity name in the Temporal activity-type string. */
    public static final String PROCESS_SEP = "::";

    /**
     * Suffix appended to an activity name to form the compensation Temporal activity type. e.g.
     * {@code "leave::deductBalance#compensate"}.
     */
    public static final String COMPENSATE_SUFFIX = "#compensate";

    /**
     * Separator between step name and hook type in hook activity names. e.g. {@code
     * "leave::manager:before"}.
     */
    public static final String HOOK_SEP = ":";

    /**
     * A registered activity bound to its S/E/C types, for the dynamic activity dispatcher. Carries
     * a flag indicating whether this binding represents the forward run or a compensation.
     */
    public static final class ActivityBinding {
        private final WorkflowActivity<?, ?, ?> activity;
        private final boolean isCompensation;
        private final Class<?> stateType;
        private final Class<?> eventType;
        private final Class<?> ctxType;

        ActivityBinding(
                WorkflowActivity<?, ?, ?> activity,
                boolean isCompensation,
                Class<?> stateType,
                Class<?> eventType,
                Class<?> ctxType) {
            this.activity = activity;
            this.isCompensation = isCompensation;
            this.stateType = stateType;
            this.eventType = eventType;
            this.ctxType = ctxType;
        }

        /**
         * Execute either the forward run or the compensate, depending on how this was registered.
         *
         * @param step the hook step name, or null for non-hook activities
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void execute(Object from, Object to, Object event, Object ctx, String step) {
            NodeContext nodeCtx = new NodeContext(from, to, event, ctx, step);
            if (isCompensation) {
                activity.compensate(nodeCtx);
            } else {
                activity.run(nodeCtx);
            }
        }

        public Class<?> stateType() {
            return stateType;
        }

        public Class<?> eventType() {
            return eventType;
        }

        public Class<?> ctxType() {
            return ctxType;
        }
    }

    private static final Map<String, ProcessDefinition<?, ?, ?>> DEFS = new ConcurrentHashMap<>();

    /** Compiled flow per process id (the resolved transition model). */
    private static final Map<String, ProcessFlow<?, ?, ?>> FLOWS = new ConcurrentHashMap<>();

    /**
     * Activity bindings keyed by NAMESPACED Temporal activity-type name: {@code processId + "::" +
     * activityName} (forward) {@code processId + "::" + activityName + "#compensate"}
     * (compensation).
     */
    private static final Map<String, ActivityBinding> ACTIVITIES = new ConcurrentHashMap<>();

    private ProcessRegistry() {}

    /**
     * Registers a process: stores the definition, calls {@link ProcessDefinition#define} once,
     * indexes its activities (namespaced forward + compensation), and stores the compiled flow.
     *
     * <p>Idempotent per id — only the first registration has effect.
     *
     * <p>States must be an {@code enum}. Activity {@link WorkflowActivity#name()} must be unique
     * within the process; a duplicate throws {@link IllegalStateException} with a clear message.
     */
    public static <S, E, C> void register(ProcessDefinition<S, E, C> def) {
        boolean firstRegistration = DEFS.put(def.id(), def) == null;

        Class<S> stateType = def.stateType();
        Class<E> eventType = def.eventType();
        Class<C> ctxType = def.ctxType();

        if (stateType.getEnumConstants() == null) {
            throw new IllegalArgumentException(
                    "workflow requires enum state types; got " + stateType);
        }

        if (!firstRegistration) {
            return;
        }

        // Build the flow exactly once
        ProcessFlow<S, E, C> flow = new ProcessFlow<>();
        def.define(flow);
        FLOWS.put(def.id(), flow);

        Map<S, ApprovalConfig<S, E, C>> approvals = flow.approvals();

        // Cross-validation: an approval state must NOT also have plain transitions out of it
        for (ProcessFlow.TransitionSpec<S, E, C> spec : flow.transitions()) {
            if (approvals.containsKey(spec.from)) {
                throw new IllegalStateException(
                        "workflow process ["
                                + def.id()
                                + "]: state ["
                                + spec.from
                                + "] is declared as an approval state but also has a plain"
                                + " transition out via event ["
                                + spec.event
                                + "]. An approval state may not have plain transitions.");
            }
        }

        // Index activities — keyed by processId-namespaced name, checking for duplicates
        Map<String, String> seenNames = new ConcurrentHashMap<>();

        for (ProcessFlow.TransitionSpec<S, E, C> spec : flow.transitions()) {
            indexActivity(def.id(), spec.activity, seenNames, stateType, eventType, ctxType);
        }

        // Index approval outcome activities and hook activities
        for (ApprovalConfig<S, E, C> cfg : approvals.values()) {
            indexActivity(
                    def.id(), cfg.approvedActivity(), seenNames, stateType, eventType, ctxType);
            indexActivity(
                    def.id(), cfg.rejectedActivity(), seenNames, stateType, eventType, ctxType);

            // Validate unique step names in the expression tree
            Set<String> stepNames = new HashSet<>();
            collectStepNames(cfg.expr(), stepNames, def.id());

            // Walk the expression tree to index hook activities
            List<Step> steps = new ArrayList<>();
            collectSteps(cfg.expr(), steps);
            for (Step step : steps) {
                indexHookActivity(
                        def.id(),
                        step.name(),
                        "before",
                        step.beforeHook(),
                        seenNames,
                        stateType,
                        eventType,
                        ctxType);
                indexHookActivity(
                        def.id(),
                        step.name(),
                        "after",
                        step.afterHook(),
                        seenNames,
                        stateType,
                        eventType,
                        ctxType);
                indexHookActivity(
                        def.id(),
                        step.name(),
                        "onComplete",
                        step.onCompleteHook(),
                        seenNames,
                        stateType,
                        eventType,
                        ctxType);
            }
        }
    }

    /**
     * Recursively collect all unique step names from the expression tree. Throws on duplicate step
     * names (same name in two leaves = ambiguous vote routing).
     */
    private static void collectStepNames(Expr expr, Set<String> stepNames, String processId) {
        if (expr instanceof Step s) {
            if (!stepNames.add(s.name())) {
                throw new IllegalStateException(
                        "workflow process ["
                                + processId
                                + "]: duplicate step name \""
                                + s.name()
                                + "\" in approval expression. Each step name must be unique.");
            }
        } else if (expr instanceof And and) {
            for (Expr child : and.children()) collectStepNames(child, stepNames, processId);
        } else if (expr instanceof Or or) {
            for (Expr child : or.children()) collectStepNames(child, stepNames, processId);
        } else if (expr instanceof AtLeast al) {
            for (Expr child : al.children()) collectStepNames(child, stepNames, processId);
        }
    }

    /** Recursively collect all Step leaf nodes (pre-order). */
    private static void collectSteps(Expr expr, List<Step> out) {
        if (expr instanceof Step s) {
            out.add(s);
        } else if (expr instanceof And and) {
            for (Expr child : and.children()) collectSteps(child, out);
        } else if (expr instanceof Or or) {
            for (Expr child : or.children()) collectSteps(child, out);
        } else if (expr instanceof AtLeast al) {
            for (Expr child : al.children()) collectSteps(child, out);
        }
    }

    /**
     * Build the hook activity name: {@code stepName + ":" + hookType} (e.g. {@code
     * "manager:before"}).
     */
    public static String hookActivityName(String stepName, String hookType) {
        return stepName + HOOK_SEP + hookType;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <S, E, C> void indexHookActivity(
            String processId,
            String stepName,
            String hookType,
            WorkflowActivity<?, ?, ?> activity,
            Map<String, String> seenNames,
            Class<S> stateType,
            Class<E> eventType,
            Class<C> ctxType) {
        if (activity == null) {
            return;
        }
        // Register under the hook-namespaced name; use the activity's own name() for uniqueness
        // checking so that hook activities can't collide with other activities by name.
        String hookName = hookActivityName(stepName, hookType);
        // Also enforce the hook activity's declared name() is unique
        String actName = activity.name();
        String prev = seenNames.put(actName, actName);
        if (prev != null) {
            throw new IllegalStateException(
                    "workflow process ["
                            + processId
                            + "]: duplicate activity name \""
                            + actName
                            + "\". Activity names must be unique within a process.");
        }
        String forwardKey = namespacedName(processId, hookName);
        ACTIVITIES.put(
                forwardKey,
                new ActivityBinding(
                        (WorkflowActivity) activity, false, stateType, eventType, ctxType));
        if (activity.compensable()) {
            ACTIVITIES.put(
                    forwardKey + COMPENSATE_SUFFIX,
                    new ActivityBinding(
                            (WorkflowActivity) activity, true, stateType, eventType, ctxType));
        }
    }

    private static <S, E, C> void indexActivity(
            String processId,
            WorkflowActivity<S, E, C> activity,
            Map<String, String> seenNames,
            Class<S> stateType,
            Class<E> eventType,
            Class<C> ctxType) {
        if (activity == null) {
            return; // routing-only or no outcome activity
        }
        String actName = activity.name();
        String prev = seenNames.put(actName, actName);
        if (prev != null) {
            throw new IllegalStateException(
                    "workflow process ["
                            + processId
                            + "]: duplicate activity name \""
                            + actName
                            + "\". Activity names must be unique within a process.");
        }
        String forwardKey = namespacedName(processId, actName);
        ACTIVITIES.put(
                forwardKey, new ActivityBinding(activity, false, stateType, eventType, ctxType));
        if (activity.compensable()) {
            ACTIVITIES.put(
                    forwardKey + COMPENSATE_SUFFIX,
                    new ActivityBinding(activity, true, stateType, eventType, ctxType));
        }
    }

    /**
     * Build the namespaced Temporal activity-type name for the forward call. {@code processId +
     * "::" + activityName}.
     */
    public static String namespacedName(String processId, String activityName) {
        return processId + PROCESS_SEP + activityName;
    }

    /**
     * Returns the compiled {@link ProcessFlow} for the given process id.
     *
     * @throws IllegalStateException if the id is not registered
     */
    @SuppressWarnings("unchecked")
    public static <S, E, C> ProcessFlow<S, E, C> flow(String processId) {
        ProcessFlow<?, ?, ?> flow = FLOWS.get(processId);
        if (flow == null) {
            throw new IllegalStateException(
                    "No workflow ProcessFlow registered for id=" + processId);
        }
        return (ProcessFlow<S, E, C>) flow;
    }

    public static ProcessDefinition<?, ?, ?> definition(String id) {
        ProcessDefinition<?, ?, ?> def = DEFS.get(id);
        if (def == null) {
            throw new IllegalStateException(
                    "No workflow ProcessDefinition registered for id=" + id);
        }
        return def;
    }

    /**
     * Look up an activity binding by its namespaced Temporal activity-type name. The name is
     * {@code processId + "::" + activity.name()} (forward) or {@code processId + "::" +
     * activity.name() + "#compensate"} (compensation).
     */
    public static ActivityBinding activity(String activityTypeName) {
        ActivityBinding binding = ACTIVITIES.get(activityTypeName);
        if (binding == null) {
            throw new IllegalStateException(
                    "No workflow activity registered with name=" + activityTypeName);
        }
        return binding;
    }

    public static boolean contains(String id) {
        return DEFS.containsKey(id);
    }

    /**
     * Returns the {@link ApprovalConfig} for {@code state} in process {@code processId}, or {@code
     * null} if that state is not an approval state. Engine uses this on each state entry.
     */
    @SuppressWarnings("unchecked")
    public static <S, E, C> ApprovalConfig<S, E, C> approval(String processId, Object state) {
        ProcessFlow<?, ?, ?> flow = FLOWS.get(processId);
        if (flow == null) {
            return null;
        }
        return (ApprovalConfig<S, E, C>) flow.approvals().get(state);
    }
}
