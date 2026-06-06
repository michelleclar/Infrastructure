package org.carl.infrastructure.workflow.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

/**
 * Verifies the {@link BuiltInNodes} sugar layer produces {@link NodeConfig}s equivalent to the
 * hand-written {@code b.type(...).set(...)} form.
 */
class BuiltInNodesTest {

    private static NodeConfig apply(Consumer<NodeBuilder> c) {
        NodeBuilder b = new NodeBuilder();
        c.accept(b);
        return b.buildConfig();
    }

    @Test
    void serviceMatchesManualBuilder() {
        NodeConfig sugar = apply(BuiltInNodes.service("createOrder"));
        NodeConfig manual =
                apply(b -> b.type(NodeTypes.SERVICE_TASK).set("activity", "createOrder"));
        assertEquals(manual, sugar);
        assertEquals(NodeTypes.SERVICE_TASK, sugar.type());
        assertEquals("createOrder", sugar.props().get("activity"));
    }

    @Test
    void approvalMatchesManualBuilder() {
        NodeConfig sugar = apply(BuiltInNodes.approval("hr"));
        NodeConfig manual = apply(b -> b.type(NodeTypes.APPROVAL_TASK).set("assignee", "hr"));
        assertEquals(manual, sugar);
        assertEquals(NodeTypes.APPROVAL_TASK, sugar.type());
        assertEquals("hr", sugar.props().get("assignee"));
    }

    @Test
    void timerMatchesManualBuilder() {
        NodeConfig sugar = apply(BuiltInNodes.timer("PT10M"));
        NodeConfig manual = apply(b -> b.type(NodeTypes.TIMER_TASK).set("duration", "PT10M"));
        assertEquals(manual, sugar);
        assertEquals(NodeTypes.TIMER_TASK, sugar.type());
        assertEquals("PT10M", sugar.props().get("duration"));
    }

    @Test
    void gatewayMatchesManualBuilder() {
        NodeConfig sugar = apply(BuiltInNodes.gateway());
        NodeConfig manual = apply(b -> b.type(NodeTypes.GATEWAY));
        assertEquals(manual, sugar);
        assertEquals(NodeTypes.GATEWAY, sugar.type());
        assertEquals(0, sugar.props().size());
    }

    @Test
    void endTaskMatchesManualBuilder() {
        NodeConfig sugar = apply(BuiltInNodes.endTask());
        NodeConfig manual = apply(b -> b.type(NodeTypes.END_TASK));
        assertEquals(manual, sugar);
        assertEquals(NodeTypes.END_TASK, sugar.type());
        assertEquals(0, sugar.props().size());
    }
}
