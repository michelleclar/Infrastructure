package org.carl.infrastructure.workflow.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class NodeHandlerRegistryTest {

    @Test
    void registerAndLookupRoundTrip() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        NodeHandler<Void> handler = serviceTaskHandler();

        registry.register(handler);

        assertSame(handler, registry.lookup(NodeTypes.SERVICE_TASK));
        assertEquals(Set.of(NodeTypes.SERVICE_TASK), registry.registeredTypes());
    }

    @Test
    void duplicateRegistrationFailsFast() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.register(serviceTaskHandler());

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class, () -> registry.register(serviceTaskHandler()));
        assertTrue(ex.getMessage().contains(NodeTypes.SERVICE_TASK));
    }

    @Test
    void lookupOfUnknownTypeThrows() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.lookup("noSuchType"));
    }

    @Test
    void findReturnsOptional() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        assertTrue(registry.find("missing").isEmpty());
        assertTrue(registry.find(null).isEmpty(), "null type yields empty Optional");

        NodeHandler<Void> handler = serviceTaskHandler();
        registry.register(handler);
        Optional<NodeHandler<?>> found = registry.find(NodeTypes.SERVICE_TASK);
        assertTrue(found.isPresent());
        assertSame(handler, found.get());
    }

    @Test
    void registerRejectsNullHandler() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        assertThrows(NullPointerException.class, () -> registry.register(null));
    }

    @Test
    void lookupRejectsNullType() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        assertThrows(NullPointerException.class, () -> registry.lookup(null));
    }

    @Test
    void registeredTypesIsUnmodifiable() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.register(serviceTaskHandler());
        Set<String> types = registry.registeredTypes();
        assertEquals(1, types.size());
        assertThrows(UnsupportedOperationException.class, () -> types.add("evilType"));
    }

    @Test
    void multipleDistinctHandlersCoexist() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.register(serviceTaskHandler());
        registry.register(
                new NodeHandler<Void>() {
                    @Override
                    public String type() {
                        return NodeTypes.APPROVAL_TASK;
                    }

                    @Override
                    public Class<Void> configType() {
                        return Void.class;
                    }

                    @Override
                    public Set<String> outcomes() {
                        return Set.of(Outcomes.APPROVED, Outcomes.REJECTED);
                    }

                    @Override
                    public NodeResult run(NodeExecutionContext ctx, Void config) {
                        return NodeResult.waiting();
                    }
                });

        assertEquals(2, registry.registeredTypes().size());
        assertTrue(registry.registeredTypes().contains(NodeTypes.SERVICE_TASK));
        assertTrue(registry.registeredTypes().contains(NodeTypes.APPROVAL_TASK));
        assertFalse(registry.registeredTypes().contains(NodeTypes.TASK_GROUP));
    }

    @Test
    void registerAlwaysRejectsForbiddenHandler() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        assertThrows(
                IllegalStateException.class,
                () -> registry.register(new DirtyHandler()),
                "register() must reject handlers that reference forbidden types");
        assertTrue(registry.registeredTypes().isEmpty());
    }

    @Test
    void registerBuiltInSkipsGuardAndAcceptsDirtyHandler() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.registerBuiltIn(new DirtyHandler());
        assertTrue(registry.registeredTypes().contains("dirtyForRegistryTest"));
    }

    /** Handler that uses UUID.randomUUID() — forbidden by DeterminismGuard. */
    static final class DirtyHandler implements NodeHandler<Void> {
        @Override
        public String type() {
            return "dirtyForRegistryTest";
        }

        @Override
        public Class<Void> configType() {
            return Void.class;
        }

        @Override
        public Set<String> outcomes() {
            return Set.of(Outcomes.SUCCESS);
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, Void config) {
            String id = UUID.randomUUID().toString();
            return NodeResult.completed(id == null ? Outcomes.FAILED : Outcomes.SUCCESS);
        }
    }

    private static NodeHandler<Void> serviceTaskHandler() {
        return new NodeHandler<Void>() {
            @Override
            public String type() {
                return NodeTypes.SERVICE_TASK;
            }

            @Override
            public Class<Void> configType() {
                return Void.class;
            }

            @Override
            public Set<String> outcomes() {
                return Set.of(Outcomes.SUCCESS, Outcomes.FAILED);
            }

            @Override
            public NodeResult run(NodeExecutionContext ctx, Void config) {
                return NodeResult.completed(Outcomes.SUCCESS);
            }
        };
    }
}
