package org.carl.infrastructure.persistence.utils;

import java.util.List;

public record DdlPrecheckResult(boolean success, List<DdlIssue> warnings, List<DdlIssue> errors) {
    public DdlPrecheckResult {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }

    public static DdlPrecheckResult success(List<DdlIssue> warnings) {
        return new DdlPrecheckResult(true, warnings, List.of());
    }

    public static DdlPrecheckResult failure(List<DdlIssue> errors) {
        return new DdlPrecheckResult(false, List.of(), errors);
    }
}
