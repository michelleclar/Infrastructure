package org.carl.infrastructure.persistence.utils;

import java.util.List;

public record DdlReverseGeneratorResult(
        boolean success,
        SchemaModel.Database database,
        String ddl,
        List<DdlIssue> warnings,
        List<DdlIssue> errors) {
    public DdlReverseGeneratorResult {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }

    public static DdlReverseGeneratorResult success(
            SchemaModel.Database database, String ddl, List<DdlIssue> warnings) {
        return new DdlReverseGeneratorResult(true, database, ddl, warnings, List.of());
    }

    public static DdlReverseGeneratorResult failure(SchemaModel.Database database, List<DdlIssue> errors) {
        return new DdlReverseGeneratorResult(false, database, null, List.of(), errors);
    }
}
