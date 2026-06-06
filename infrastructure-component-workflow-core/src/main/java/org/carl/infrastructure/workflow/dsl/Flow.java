package org.carl.infrastructure.workflow.dsl;

/**
 * Entry point for the flow-first DSL.
 *
 * <pre>
 * import static org.carl.infrastructure.workflow.dsl.Dsl.*;
 *
 * FlowDef flow = Flow.define("leaveV2", "请假流程");
 * flow.start("发起请假");
 * // ... declare nodes and edges ...
 * WorkflowDefinition def = flow.build();
 * </pre>
 */
public final class Flow {

    private Flow() {
        throw new AssertionError("no instances");
    }

    /**
     * Creates a new {@link FlowDef} accumulator with the given workflow id and display name.
     *
     * @param id stable workflow identifier (used in Temporal task queues / history)
     * @param name human-readable display name
     * @return a fresh {@link FlowDef}
     */
    public static FlowDef define(String id, String name) {
        return new FlowDef(id, name);
    }
}
