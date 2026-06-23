package org.carl.infrastructure.persistence.utils;

import java.util.List;

public record DdlParserResult(
        boolean success, SchemaModel.Database database, List<DdlIssue> warnings, List<DdlIssue> errors) {
    public DdlParserResult {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }

    public static DdlParserResult success(SchemaModel.Database database, List<DdlIssue> warnings) {
        return new DdlParserResult(true, database, warnings, List.of());
    }

    public static DdlParserResult failure(List<DdlIssue> errors) {
        return new DdlParserResult(false, null, List.of(), errors);
    }
}
