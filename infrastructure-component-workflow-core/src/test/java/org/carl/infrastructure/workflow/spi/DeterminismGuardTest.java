package org.carl.infrastructure.workflow.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class DeterminismGuardTest {

    @Test
    void cleanHandlerHasNoViolations() {
        List<String> violations = DeterminismGuard.staticScan(CleanHandler.class);
        assertTrue(
                violations.isEmpty(),
                () -> "expected no violations for CleanHandler, got: " + violations);
    }

    @Test
    void handlerCallingSystemCurrentTimeMillisIsFlagged() {
        List<String> violations = DeterminismGuard.staticScan(DirtyClockHandler.class);
        assertFalse(violations.isEmpty(), "expected violations for DirtyClockHandler");
        assertTrue(
                violations.stream().anyMatch(v -> v.contains("java.lang.System")),
                () -> "expected java.lang.System violation, got: " + violations);
        assertTrue(
                violations.stream().anyMatch(v -> v.contains("java.lang.System#currentTimeMillis")),
                () -> "expected currentTimeMillis method violation, got: " + violations);
    }

    @Test
    void handlerUsingRandomUUIDIsFlagged() {
        List<String> violations = DeterminismGuard.staticScan(DirtyUuidHandler.class);
        assertFalse(violations.isEmpty(), "expected violations for DirtyUuidHandler");
        assertTrue(
                violations.stream().anyMatch(v -> v.contains("java.util.UUID")),
                () -> "expected java.util.UUID violation, got: " + violations);
    }

    @Test
    void assertPureSucceedsForCleanHandler() {
        assertDoesNotThrow(() -> DeterminismGuard.assertPure(CleanHandler.class));
    }

    @Test
    void assertPureThrowsForDirtyHandler() {
        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> DeterminismGuard.assertPure(DirtyClockHandler.class));
        assertTrue(
                ex.getMessage().contains(DirtyClockHandler.class.getName()),
                () -> "exception message should mention handler class, got: " + ex.getMessage());
        assertTrue(
                ex.getMessage().contains("java.lang.System"),
                () -> "exception message should mention forbidden type, got: " + ex.getMessage());
    }

    @Test
    void forbiddenSetsAreNotEmpty() {
        // sanity: make sure constants weren't accidentally cleared.
        assertFalse(DeterminismGuard.FORBIDDEN_TYPES.isEmpty());
        assertFalse(DeterminismGuard.FORBIDDEN_METHODS.isEmpty());
        // java.lang.System is deliberately NOT type-level forbidden (System.arraycopy etc. are
        // benign); its unsafe members are covered at method granularity instead.
        assertFalse(DeterminismGuard.FORBIDDEN_TYPES.contains("java.lang.System"));
        assertTrue(
                DeterminismGuard.FORBIDDEN_METHODS.contains("java.lang.System#currentTimeMillis"));
    }

    @Test
    void registryStrictModeRejectsDirtyHandlerOnRegister() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry().strict();
        assertTrue(registry.isStrict());
        assertThrows(IllegalStateException.class, () -> registry.register(new DirtyClockHandler()));
        // dirty type should NOT be in the registry after the failed register call.
        assertEquals(Set.of(), registry.registeredTypes());
    }

    @Test
    void registryDefaultModeAlwaysRejectsDirtyHandler() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        assertTrue(registry.isStrict(), "register() is always strict");
        assertThrows(IllegalStateException.class, () -> registry.register(new DirtyClockHandler()));
        assertEquals(Set.of(), registry.registeredTypes());
    }

    @Test
    void registerBuiltInBypassesGuard() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        // registerBuiltIn skips the guard — dirty handler is accepted
        registry.registerBuiltIn(new DirtyClockHandler());
        assertEquals(Set.of("dirtyClock"), registry.registeredTypes());
    }

    @Test
    void registryStrictModeAcceptsCleanHandler() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry().strict();
        registry.register(new CleanHandler());
        assertEquals(Set.of("clean"), registry.registeredTypes());
    }

    /** A handler that touches no forbidden JDK APIs. */
    static final class CleanHandler implements NodeHandler<Void, Object, Object> {
        @Override
        public String type() {
            return "clean";
        }

        @Override
        public Class<Void> configType() {
            return Void.class;
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, Void config) {
            return NodeResult.completed("SUCCESS");
        }
    }

    /** A handler that intentionally violates the determinism contract by reading wall clock. */
    static final class DirtyClockHandler implements NodeHandler<Void, Object, Object> {
        @Override
        public String type() {
            return "dirtyClock";
        }

        @Override
        public Class<Void> configType() {
            return Void.class;
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, Void config) {
            long now = System.currentTimeMillis();
            if (now > 0) {
                return NodeResult.completed("SUCCESS");
            }
            return NodeResult.waiting();
        }
    }

    /** A handler that intentionally violates by generating a random UUID. */
    static final class DirtyUuidHandler implements NodeHandler<Void, Object, Object> {
        @Override
        public String type() {
            return "dirtyUuid";
        }

        @Override
        public Class<Void> configType() {
            return Void.class;
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, Void config) {
            String id = java.util.UUID.randomUUID().toString();
            return NodeResult.completed(id == null ? "FAILED" : "SUCCESS");
        }
    }
}
