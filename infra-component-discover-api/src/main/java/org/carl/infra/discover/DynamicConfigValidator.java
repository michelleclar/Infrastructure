package org.carl.infra.discover;

/** Validates a complete dynamic-configuration snapshot before it becomes visible. */
@FunctionalInterface
public interface DynamicConfigValidator {

    void validate(DynamicConfigSnapshot snapshot);
}
