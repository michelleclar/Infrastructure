package org.carl.infrastructure.workflow.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

class WorkflowInterceptorRegistryTest {

    @Test
    void registerDeterministicAppearsOnlyInDeterministicList() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        DeterministicInterceptor det = new DeterministicInterceptor() {};

        registry.register(det);

        assertTrue(registry.deterministic().contains(det));
        assertFalse(registry.async().contains(det));
        assertEquals(1, registry.deterministic().size());
        assertEquals(0, registry.async().size());
    }

    @Test
    void registerAsyncAppearsOnlyInAsyncList() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        AsyncInterceptor async = new AsyncInterceptor() {};

        registry.register(async);

        assertTrue(registry.async().contains(async));
        assertFalse(registry.deterministic().contains(async));
        assertEquals(1, registry.async().size());
        assertEquals(0, registry.deterministic().size());
    }

    @Test
    void registerDualInterceptorAppearsInBothLists() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        DualInterceptor dual = new DualInterceptor();

        registry.register(dual);

        assertEquals(1, registry.deterministic().size());
        assertEquals(1, registry.async().size());
        assertSame(dual, registry.deterministic().get(0));
        assertSame(dual, registry.async().get(0));
    }

    @Test
    void registerRawInterceptorRejected() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        WorkflowInterceptor raw = new WorkflowInterceptor() {};

        assertThrows(IllegalArgumentException.class, () -> registry.register(raw));
        assertEquals(0, registry.size());
    }

    @Test
    void deterministicListSortedByOrderAscending() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        DeterministicInterceptor low = new OrderedDeterministic(-5);
        DeterministicInterceptor mid = new OrderedDeterministic(0);
        DeterministicInterceptor high = new OrderedDeterministic(10);

        // Register out of order.
        registry.register(high);
        registry.register(low);
        registry.register(mid);

        List<DeterministicInterceptor> sorted = registry.deterministic();
        assertEquals(3, sorted.size());
        assertSame(low, sorted.get(0));
        assertSame(mid, sorted.get(1));
        assertSame(high, sorted.get(2));
    }

    @Test
    void asyncListSortedByOrderAscending() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        AsyncInterceptor low = new OrderedAsync(-5);
        AsyncInterceptor mid = new OrderedAsync(0);
        AsyncInterceptor high = new OrderedAsync(10);

        registry.register(high);
        registry.register(low);
        registry.register(mid);

        List<AsyncInterceptor> sorted = registry.async();
        assertEquals(3, sorted.size());
        assertSame(low, sorted.get(0));
        assertSame(mid, sorted.get(1));
        assertSame(high, sorted.get(2));
    }

    @Test
    void registerNullThrowsNpe() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        assertThrows(NullPointerException.class, () -> registry.register(null));
    }

    @Test
    void sizeCountsBothLists() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        assertEquals(0, registry.size());

        registry.register(new DeterministicInterceptor() {});
        assertEquals(1, registry.size());

        registry.register(new AsyncInterceptor() {});
        assertEquals(2, registry.size());

        registry.register(new DualInterceptor());
        // dual lives in both lists, so size() counts it twice (sum of list sizes).
        assertEquals(4, registry.size());
    }

    @Test
    void clearEmptiesBothLists() {
        WorkflowInterceptorRegistry registry = new WorkflowInterceptorRegistry();
        registry.register(new DeterministicInterceptor() {});
        registry.register(new AsyncInterceptor() {});
        registry.register(new DualInterceptor());

        registry.clear();

        assertTrue(registry.deterministic().isEmpty());
        assertTrue(registry.async().isEmpty());
        assertEquals(0, registry.size());
    }

    /**
     * Implements both deterministic and async to exercise dual-list registration. Since both
     * super-interfaces declare matching default hooks, every hook must be explicitly resolved here;
     * we route to the deterministic side as a deliberate test-only choice.
     */
    static final class DualInterceptor implements DeterministicInterceptor, AsyncInterceptor {
        @Override
        public void onWorkflowStart(InterceptorContext ctx) {
            DeterministicInterceptor.super.onWorkflowStart(ctx);
        }

        @Override
        public void onWorkflowEnd(InterceptorContext ctx, NodeResult terminalResult) {
            DeterministicInterceptor.super.onWorkflowEnd(ctx, terminalResult);
        }

        @Override
        public void onNodeEnter(InterceptorContext ctx, NodeDefinition node) {
            DeterministicInterceptor.super.onNodeEnter(ctx, node);
        }

        @Override
        public void onNodeExit(InterceptorContext ctx, NodeDefinition node, NodeResult result) {
            DeterministicInterceptor.super.onNodeExit(ctx, node, result);
        }

        @Override
        public void onNodeError(InterceptorContext ctx, NodeDefinition node, String errorMessage) {
            DeterministicInterceptor.super.onNodeError(ctx, node, errorMessage);
        }

        @Override
        public void onEvent(InterceptorContext ctx, WorkflowEvent event) {
            DeterministicInterceptor.super.onEvent(ctx, event);
        }

        @Override
        public void onCompensate(
                InterceptorContext ctx, NodeDefinition node, NodeResult originalResult) {
            DeterministicInterceptor.super.onCompensate(ctx, node, originalResult);
        }
    }

    static final class OrderedDeterministic implements DeterministicInterceptor {
        private final int order;

        OrderedDeterministic(int order) {
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }
    }

    static final class OrderedAsync implements AsyncInterceptor {
        private final int order;

        OrderedAsync(int order) {
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }
    }
}
