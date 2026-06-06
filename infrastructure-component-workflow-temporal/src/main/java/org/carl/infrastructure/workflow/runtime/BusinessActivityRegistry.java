package org.carl.infrastructure.workflow.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.Map;

/**
 * Thread-safe registry of business activity functions keyed by name.
 *
 * <p>Activities are plain {@code Map<String,Object> -> Map<String,Object>} functions and are
 * invoked from {@link GenericActivityImpl} when {@link ServiceTaskHandler} requests an activity by
 * name.
 *
 * <p>Lookup semantics: {@link #lookup(String)} throws {@link IllegalArgumentException} on miss to
 * surface configuration mistakes early.
 */
public final class BusinessActivityRegistry {

    private final ConcurrentMap<String, Function<Map<String, Object>, Map<String, Object>>>
            functions = new ConcurrentHashMap<>();

    public void register(
            String activityName, Function<Map<String, Object>, Map<String, Object>> fn) {
        Objects.requireNonNull(activityName, "activityName");
        if (activityName.isBlank()) {
            throw new IllegalArgumentException("activityName must not be blank");
        }
        Objects.requireNonNull(fn, "fn");
        Function<Map<String, Object>, Map<String, Object>> previous =
                functions.putIfAbsent(activityName, fn);
        if (previous != null) {
            throw new IllegalStateException("Activity already registered: " + activityName);
        }
    }

    public Function<Map<String, Object>, Map<String, Object>> lookup(String name) {
        Objects.requireNonNull(name, "name");
        Function<Map<String, Object>, Map<String, Object>> fn = functions.get(name);
        if (fn == null) {
            throw new IllegalArgumentException("No activity registered for name: " + name);
        }
        return fn;
    }

    public Optional<Function<Map<String, Object>, Map<String, Object>>> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(functions.get(name));
    }
}
