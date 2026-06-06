package org.carl.infrastructure.workflow.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Default {@link GenericActivity} implementation.
 *
 * <p>Looks up the business function in the provided {@link BusinessActivityRegistry} and invokes it
 * with the call input. Any thrown {@link RuntimeException} is turned into a failed {@link
 * ActivityResult} so the orchestrating workflow can route it through {@code
 * ServiceTaskHandler.onEvent} without surfacing as a Temporal activity exception.
 */
public final class GenericActivityImpl implements GenericActivity {

    private final BusinessActivityRegistry registry;

    public GenericActivityImpl(BusinessActivityRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public ActivityResult execute(ActivityCall call) {
        if (call == null || call.activityName() == null || call.activityName().isBlank()) {
            return new ActivityResult(false, Map.of(), "activity name is missing");
        }
        Function<Map<String, Object>, Map<String, Object>> fn;
        try {
            fn = registry.lookup(call.activityName());
        } catch (RuntimeException lookupFailure) {
            return new ActivityResult(false, Map.of(), lookupFailure.getMessage());
        }
        Map<String, Object> input =
                call.input() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(call.input());
        try {
            Map<String, Object> out = fn.apply(input);
            Map<String, Object> safe = out == null ? Map.of() : Map.copyOf(out);
            return new ActivityResult(true, safe, null);
        } catch (RuntimeException businessFailure) {
            String msg = businessFailure.getMessage();
            return new ActivityResult(false, Map.of(), msg == null ? "activity threw" : msg);
        }
    }
}
