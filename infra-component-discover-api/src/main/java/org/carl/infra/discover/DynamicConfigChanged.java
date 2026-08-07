package org.carl.infra.discover;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Complete dynamic-configuration transition. */
public record DynamicConfigChanged(
        DynamicConfigSnapshot previous,
        DynamicConfigSnapshot current,
        Set<String> changedKeys) {

    public DynamicConfigChanged {
        if (previous == null || current == null) {
            throw new IllegalArgumentException("configuration snapshots must not be null");
        }
        changedKeys =
                Collections.unmodifiableSet(
                        new TreeSet<>(changedKeys == null ? Set.of() : changedKeys));
    }
}
