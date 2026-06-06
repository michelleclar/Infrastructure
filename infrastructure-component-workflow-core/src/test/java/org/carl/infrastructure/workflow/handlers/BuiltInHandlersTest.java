package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.junit.jupiter.api.Test;

import java.util.Set;

class BuiltInHandlersTest {

    @Test
    void registerAllPopulatesNineTypes() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(registry);

        Set<String> types = registry.registeredTypes();
        assertEquals(9, types.size(), "should register exactly nine built-in handlers");
        assertTrue(types.contains(NodeTypes.SERVICE_TASK));
        assertTrue(types.contains(NodeTypes.APPROVAL_TASK));
        assertTrue(types.contains(NodeTypes.USER_TASK));
        assertTrue(types.contains(NodeTypes.EVENT_TASK));
        assertTrue(types.contains(NodeTypes.TIMER_TASK));
        assertTrue(types.contains(NodeTypes.TASK_GROUP));
        assertTrue(types.contains(NodeTypes.GATEWAY));
        assertTrue(types.contains(NodeTypes.SUB_PROCESS));
        assertTrue(types.contains(NodeTypes.END_TASK));
    }

    @Test
    void registerAllOnSameRegistryTwiceFailsLoudly() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(registry);
        assertThrows(IllegalStateException.class, () -> BuiltInHandlers.registerAll(registry));
    }

    @Test
    void registerAllRejectsNullRegistry() {
        assertThrows(NullPointerException.class, () -> BuiltInHandlers.registerAll(null));
    }
}
