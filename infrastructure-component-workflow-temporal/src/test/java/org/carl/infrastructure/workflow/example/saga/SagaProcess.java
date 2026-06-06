package org.carl.infrastructure.workflow.example.saga;

import java.util.Map;

import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;

/** Static {@link WorkflowDefinition}s used by the saga compensation tests. */
final class SagaProcess {

    private SagaProcess() {
        throw new AssertionError("no instances");
    }

    /**
     * Three-step saga: createOrder → reserveBudget → sendNotification
     *
     * <ul>
     *   <li>{@code createOrder} — compensable via {@code cancelOrder} activity.
     *   <li>{@code reserveBudget} — compensable via {@code releaseBudget} activity.
     *   <li>{@code sendNotification} — NOT compensable; the test makes it fail so the two prior
     *       nodes are rolled back in reverse order.
     * </ul>
     */
    static WorkflowDefinition sagaFlow() {
        FlowDef flow = Flow.define("sagaV2", "Saga 补偿测试");
        flow.start("createOrder");

        flow.node(
                "createOrder",
                BuiltInNodes.service("createOrder")
                        .andThen(b -> b.set("compensateActivity", "cancelOrder")));
        flow.node(
                "reserveBudget",
                BuiltInNodes.service("reserveBudget")
                        .andThen(b -> b.set("compensateActivity", "releaseBudget")));
        flow.node("sendNotification", BuiltInNodes.service("sendNotification"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("已完成"));

        flow.from("createOrder").on(Outcomes.SUCCESS).to("reserveBudget");
        flow.from("reserveBudget").on(Outcomes.SUCCESS).to("sendNotification");
        flow.from("sendNotification").on(Outcomes.SUCCESS).to("completed");

        return flow.build();
    }
}
