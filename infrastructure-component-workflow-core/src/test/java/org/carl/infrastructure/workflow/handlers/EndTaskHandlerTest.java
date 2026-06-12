package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

class EndTaskHandlerTest {

    private final EndTaskHandler handler = new EndTaskHandler();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.END_TASK, handler.type());
        assertEquals(EndTaskConfig.class, handler.configType());
    }

    @Test
    void runImmediatelyCompletes() {
        NodeResult r = handler.run(new TestContext(), new EndTaskConfig());
        assertEquals(NodeStatus.COMPLETED, r.status());
        assertEquals("COMPLETED", r.outcome());
    }

    @Test
    void canAcceptDefaultsToFalse() {
        assertFalse(
                handler.canAccept(
                        new TestContext(),
                        new WorkflowEvent("anything", null),
                        new EndTaskConfig()));
    }
}
