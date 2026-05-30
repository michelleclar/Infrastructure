package org.carl.infrastructure.workflowv2.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry of processes and their compiled {@link ProcessFlow}s. Intentionally static:
 * Temporal instantiates {@link org.carl.infrastructure.workflowv2.engine.GenericWorkflow} outside
 * CDI, so the engine reaches definitions via this singleton. Wired from CDI at startup (see runtime
 * bootstrap) or directly in tests.
 *
 * <h3>Activity-name namespacing</h3>
 *
 * <p>Activity names are SCOPED to a process. The Temporal activity-type name used on the wire is:
 * <pre>
 *   forward call:      {@code processId + "::" + activity.name()}
 *   compensation call: {@code processId + "::" + activity.name() + "#compensate"}
 * </pre>
 * This prevents name collisions across processes and makes dispatch unambiguous.
 * Constants: {@link #PROCESS_SEP} ({@code "::"}) and {@link #COMPENSATE_SUFFIX} ({@code "#compensate"}).
 *
 * <h3>Duplicate detection</h3>
 *
 * <p>Within a single process, {@link WorkflowActivity#name()} must be unique. A clear error is
 * thrown at registration time if the same name appears in two different transitions.
 */
public final class ProcessRegistry {

    /** Separator between processId and activity name in the Temporal activity-type string. */
    public static final String PROCESS_SEP = "::";

    /**
     * Suffix appended to an activity name to form the compensation Temporal activity type.
     * e.g. {@code "leave::deductBalance#compensate"}.
     */
    public static final String COMPENSATE_SUFFIX = "#compensate";

    /**
     * A registered activity bound to its S/E/C types, for the dynamic activity dispatcher.
     * Carries a flag indicating whether this binding represents the forward run or a compensation.
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

        /** Execute either the forward run or the compensate, depending on how this was registered. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void execute(Object from, Object to, Object event, Object ctx) {
            NodeContext nodeCtx = new NodeContext(from, to, event, ctx);
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

    /**
     * Compiled flow per process id (the resolved transition model).
     */
    private static final Map<String, ProcessFlow<?, ?, ?>> FLOWS = new ConcurrentHashMap<>();

    /**
     * Activity bindings keyed by NAMESPACED Temporal activity-type name:
     * {@code processId + "::" + activityName} (forward)
     * {@code processId + "::" + activityName + "#compensate"} (compensation).
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
                    "workflowV2 requires enum state types; got " + stateType);
        }

        if (!firstRegistration) {
            return;
        }

        // Build the flow exactly once
        ProcessFlow<S, E, C> flow = new ProcessFlow<>();
        def.define(flow);
        FLOWS.put(def.id(), flow);

        // Index activities — keyed by processId-namespaced name, checking for duplicates
        // Track activity names seen in this process to catch duplicates
        Map<String, String> seenNames = new ConcurrentHashMap<>();

        for (ProcessFlow.TransitionSpec<S, E, C> spec : flow.transitions()) {
            WorkflowActivity<S, E, C> activity = spec.activity;
            if (activity == null) {
                continue; // routing-only transition
            }

            String actName = activity.name();

            // Validate uniqueness within this process
            String prev = seenNames.put(actName, actName);
            if (prev != null) {
                throw new IllegalStateException(
                        "workflowV2 process ["
                                + def.id()
                                + "]: duplicate activity name \""
                                + actName
                                + "\". Activity names must be unique within a process.");
            }

            String forwardKey = namespacedName(def.id(), actName);
            String compensateKey = forwardKey + COMPENSATE_SUFFIX;

            // Forward binding
            ACTIVITIES.put(
                    forwardKey,
                    new ActivityBinding(activity, false, stateType, eventType, ctxType));

            // Compensation binding (only if the activity is compensable)
            if (activity.compensable()) {
                ACTIVITIES.put(
                        compensateKey,
                        new ActivityBinding(activity, true, stateType, eventType, ctxType));
            }
        }
    }

    /**
     * Build the namespaced Temporal activity-type name for the forward call.
     * {@code processId + "::" + activityName}.
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
                    "No workflowV2 ProcessFlow registered for id=" + processId);
        }
        return (ProcessFlow<S, E, C>) flow;
    }

    public static ProcessDefinition<?, ?, ?> definition(String id) {
        ProcessDefinition<?, ?, ?> def = DEFS.get(id);
        if (def == null) {
            throw new IllegalStateException(
                    "No workflowV2 ProcessDefinition registered for id=" + id);
        }
        return def;
    }

    /**
     * Look up an activity binding by its namespaced Temporal activity-type name. The name is
     * {@code processId + "::" + activity.name()} (forward) or
     * {@code processId + "::" + activity.name() + "#compensate"} (compensation).
     */
    public static ActivityBinding activity(String activityTypeName) {
        ActivityBinding binding = ACTIVITIES.get(activityTypeName);
        if (binding == null) {
            throw new IllegalStateException(
                    "No workflowV2 activity registered with name=" + activityTypeName);
        }
        return binding;
    }

    public static boolean contains(String id) {
        return DEFS.containsKey(id);
    }
}
