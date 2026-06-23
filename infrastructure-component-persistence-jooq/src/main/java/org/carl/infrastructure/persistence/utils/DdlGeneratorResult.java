package org.carl.infrastructure.persistence.utils;

import java.util.List;

public record DdlGeneratorResult(
        boolean success, String ddl, List<DdlIssue> warnings, List<DdlIssue> errors) {
    public DdlGeneratorResult {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }

    public static DdlGeneratorResult success(String ddl, List<DdlIssue> warnings) {
        return new DdlGeneratorResult(true, ddl, warnings, List.of());
    }

    public static DdlGeneratorResult failure(List<DdlIssue> errors) {
        return new DdlGeneratorResult(false, null, List.of(), errors);
    }
}
