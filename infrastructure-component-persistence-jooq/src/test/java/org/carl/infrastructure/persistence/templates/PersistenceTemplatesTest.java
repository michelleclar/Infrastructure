package org.carl.infrastructure.persistence.templates;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceTemplatesTest {

    private static final DSLContext POSTGRES_DSL = DSL.using(SQLDialect.POSTGRES);
    private static final Table<Record> DOCUMENT = DSL.table(DSL.name("document"));
    private static final Field<Long> ID = DSL.field(DSL.name("id"), Long.class);
    private static final Field<String> TITLE = DSL.field(DSL.name("title"), String.class);
    private static final Field<Integer> ROW_VERSION = DSL.field(DSL.name("row_version"), Integer.class);
    private static final Field<OffsetDateTime> DELETED_AT =
            DSL.field(DSL.name("deleted_at"), OffsetDateTime.class);
    private static final Field<String> TENANT_KEY = DSL.field(DSL.name("tenant_key"), String.class);

    @Test
    void softDeleteConditionComposesWithAdditionalFilters() {
        Condition condition =
                SoftDeleteConditions.notDeletedAnd(
                        DELETED_AT,
                        ID.eq(100L),
                        TENANT_KEY.eq("tenant-a"));

        assertEquals(
                "(\"deleted_at\" is null and \"id\" = 100 and \"tenant_key\" = 'tenant-a')",
                POSTGRES_DSL.renderInlined(condition));
    }

    @Test
    void optimisticLockUpdateSucceedsAndIncrementsVersion() {
        AtomicReference<String> sql = new AtomicReference<>();
        AtomicReference<List<Object>> bindings = new AtomicReference<>();
        DSLContext dsl =
                DSL.using(
                        new MockConnection(
                                context -> {
                                    sql.set(context.sql());
                                    bindings.set(List.of(context.bindings()));
                                    return new MockResult[] {new MockResult(1)};
                                }),
                        SQLDialect.POSTGRES);

        OptimisticLockUpdateResult result =
                OptimisticLockUpdates.execute(
                        OptimisticLockUpdates.withVersionCheckAndIncrement(
                                dsl.update(DOCUMENT).set(TITLE, "updated-title"),
                                ROW_VERSION,
                                7,
                                SoftDeleteConditions.notDeletedAnd(DELETED_AT, ID.eq(100L))));

        assertTrue(result.updated());
        assertFalse(result.conflict());
        assertEquals(1, result.affectedRows());
        assertEquals(
                "update \"document\" set \"title\" = ?, \"row_version\" = (\"row_version\" + ?) "
                        + "where (\"deleted_at\" is null and \"id\" = ? and \"row_version\" = ?)",
                sql.get());
        assertEquals(List.of("updated-title", 1, 100L, 7), bindings.get());
    }

    @Test
    void optimisticLockUpdateDetectsVersionConflict() {
        DSLContext dsl =
                DSL.using(
                        new MockConnection(context -> new MockResult[] {new MockResult(0)}),
                        SQLDialect.POSTGRES);

        OptimisticLockUpdateResult result =
                OptimisticLockUpdates.execute(
                        OptimisticLockUpdates.withVersionCheckAndIncrement(
                                dsl.update(DOCUMENT).set(TITLE, "updated-title"),
                                ROW_VERSION,
                                9,
                                SoftDeleteConditions.notDeletedAnd(DELETED_AT, ID.eq(100L))));

        assertFalse(result.updated());
        assertTrue(result.conflict());
        assertEquals(0, result.affectedRows());
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> result.throwIfConflict(() -> new IllegalStateException("version conflict")));
        assertEquals("version conflict", exception.getMessage());
    }

    @Test
    void templatesDoNotImportBusinessOrRuntimeFrameworkDependencies() throws IOException {
        Path sourceRoot = Path.of("src/main/java/org/carl/infrastructure/persistence/templates");
        if (!Files.exists(sourceRoot)) {
            sourceRoot =
                    Path.of(
                            "infrastructure-component-persistence-jooq/src/main/java/org/carl/infrastructure/persistence/templates");
        }
        String source =
                Files.walk(sourceRoot)
                        .filter(path -> path.toString().endsWith(".java"))
                        .map(
                                path -> {
                                    try {
                                        return Files.readString(path);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                        .reduce("", (left, right) -> left + "\n" + right);

        assertFalse(source.contains("import io.quarkus."));
        assertFalse(source.contains("import jakarta.enterprise."));
        assertFalse(source.contains("import jakarta.ws.rs."));
        assertFalse(source.contains("import org.eclipse.microprofile."));
        assertFalse(source.contains("import io.agroal."));
        assertFalse(source.contains("ErrorCode"));
        assertFalse(source.contains("ApiException"));
        assertFalse(source.contains("SchemaModel"));
    }
}
