package org.carl.infrastructure.workflow.interceptor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for {@link DeterministicInterceptor} and {@link AsyncInterceptor} instances. Returns
 * sorted views (by {@link WorkflowInterceptor#order()} ascending) for the runtime to invoke.
 *
 * <p>Thread-safe: backed by {@link CopyOnWriteArrayList}.
 */
public final class WorkflowInterceptorRegistry {

    private final List<DeterministicInterceptor> deterministic = new CopyOnWriteArrayList<>();
    private final List<AsyncInterceptor> async = new CopyOnWriteArrayList<>();

    public void register(WorkflowInterceptor interceptor) {
        Objects.requireNonNull(interceptor, "interceptor");
        if (interceptor instanceof DeterministicInterceptor di) {
            insertSorted(deterministic, di);
        }
        if (interceptor instanceof AsyncInterceptor ai) {
            insertSorted(async, ai);
        }
        if (!(interceptor instanceof DeterministicInterceptor)
                && !(interceptor instanceof AsyncInterceptor)) {
            throw new IllegalArgumentException(
                    "interceptor must implement DeterministicInterceptor or AsyncInterceptor: "
                            + interceptor.getClass().getName());
        }
    }

    public List<DeterministicInterceptor> deterministic() {
        return List.copyOf(deterministic);
    }

    public List<AsyncInterceptor> async() {
        return List.copyOf(async);
    }

    public int size() {
        return deterministic.size() + async.size();
    }

    public void clear() {
        deterministic.clear();
        async.clear();
    }

    private static <T extends WorkflowInterceptor> void insertSorted(List<T> list, T x) {
        // Linear insertion to maintain stable ordering by order().
        int i = 0;
        for (; i < list.size(); i++) {
            if (list.get(i).order() > x.order()) break;
        }
        list.add(i, x);
    }
}
