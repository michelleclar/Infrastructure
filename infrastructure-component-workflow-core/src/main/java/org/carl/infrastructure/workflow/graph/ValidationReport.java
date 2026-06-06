package org.carl.infrastructure.workflow.graph;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of a {@link
 * GraphValidator#validate(org.carl.infrastructure.workflow.definition.WorkflowDefinition) validate}
 * call.
 *
 * <p>Errors mean the definition is structurally invalid and must not be executed. Warnings are
 * advisory: the definition can still run but the engine cannot guarantee well-formed behaviour
 * (e.g. unreachable nodes, cycles).
 *
 * <p>The canonical constructor copies both lists via {@link List#copyOf} to keep the record
 * immutable.
 */
public record ValidationReport(List<String> errors, List<String> warnings) {

    public ValidationReport {
        Objects.requireNonNull(errors, "errors");
        Objects.requireNonNull(warnings, "warnings");
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    public boolean ok() {
        return errors.isEmpty();
    }

    public void throwIfInvalid() {
        if (!ok()) {
            throw new IllegalStateException(
                    "WorkflowDefinition validation failed: " + String.join("; ", errors));
        }
    }
}
