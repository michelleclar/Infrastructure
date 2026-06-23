package org.carl.infrastructure.persistence.utils;

import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.ColumnInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.ColumnTypeFamily;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.DatabaseInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.IndexColumnInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.IndexInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.NormalizedColumnType;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.PrimaryKeyInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.SchemaInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.TableInfo;
import org.carl.infrastructure.persistence.utils.SchemaModel.CheckConstraint;
import org.carl.infrastructure.persistence.utils.SchemaModel.Column;
import org.carl.infrastructure.persistence.utils.SchemaModel.ColumnType;
import org.carl.infrastructure.persistence.utils.SchemaModel.Database;
import org.carl.infrastructure.persistence.utils.SchemaModel.Index;
import org.carl.infrastructure.persistence.utils.SchemaModel.PrimaryKey;
import org.carl.infrastructure.persistence.utils.SchemaModel.Schema;
import org.carl.infrastructure.persistence.utils.SchemaModel.Table;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeutralDdlUtilsTest {

    @Test
    void precheckReportsExactObjectPath() {
        Database database =
                new Database(
                        null,
                        List.of(
                                new Schema(
                                        "CamelSchema",
                                        null,
                                        List.of(
                                                new Table(
                                                        "OrderLine",
                                                        null,
                                                        List.of(
                                                                new Column(
                                                                        "ID",
                                                                        new ColumnType("bigint", Types.BIGINT, null, null),
                                                                        false,
                                                                        null,
                                                                        false,
                                                                        false,
                                                                        null)),
                                                        new PrimaryKey("PK_OrderLine", List.of("MissingColumn")),
                                                        List.of(),
                                                        List.of(),
                                                        List.of())))));

        DdlPrecheckResult result = NeutralDdlUtils.precheck(database);

        assertFalse(result.success());
        assertEquals(
                "schemas[CamelSchema].tables[OrderLine].primaryKey.columns[MissingColumn]",
                result.errors().getFirst().objectPath());
    }

    @Test
    void generatesPostgresAndMysqlDdlFromNeutralModel() {
        Database database = databaseModel();

        DdlGeneratorResult postgres = NeutralDdlUtils.generate(ExternalDatabaseKind.POSTGRESQL, database);
        DdlGeneratorResult mysql = NeutralDdlUtils.generate(ExternalDatabaseKind.MYSQL, database);

        assertTrue(postgres.success());
        assertTrue(postgres.ddl().contains("create table \"CamelSchema\".\"OrderLine\""));
        assertTrue(postgres.ddl().contains("constraint \"PK_OrderLine\" primary key (\"ID\")"));
        assertTrue(postgres.ddl().contains("comment on column \"CamelSchema\".\"OrderLine\".\"UserID\" is 'user id'"));
        assertTrue(mysql.success());
        assertTrue(mysql.ddl().contains("create table `CamelSchema`.`OrderLine`"));
        assertTrue(mysql.ddl().contains("`UserID` int4(10) default 0 comment 'user id'"));
        assertTrue(mysql.ddl().contains("create index `IX_OrderLine_UserID`"));
    }

    @Test
    void parsesSupportedDdlSubset() {
        String ddl =
                """
                create table "CamelSchema"."OrderLine" (
                    "ID" bigint not null,
                    "UserID" int4 default 0,
                    constraint "PK_OrderLine" primary key ("ID"),
                    constraint "CK_OrderLine_ID" check ("ID" > 0)
                );
                create index "IX_OrderLine_UserID" on "CamelSchema"."OrderLine" ("UserID");
                comment on table "CamelSchema"."OrderLine" is 'order line table';
                comment on column "CamelSchema"."OrderLine"."UserID" is 'user id';
                """;

        DdlParserResult result = NeutralDdlUtils.parse(ExternalDatabaseKind.POSTGRESQL, ddl);

        assertTrue(result.success());
        Table table = result.database().schemas().getFirst().tables().getFirst();
        assertEquals("OrderLine", table.name());
        assertEquals("order line table", table.comment());
        assertEquals("UserID", table.columns().get(1).name());
        assertEquals("user id", table.columns().get(1).comment());
        assertEquals("PK_OrderLine", table.primaryKey().name());
        assertEquals("CK_OrderLine_ID", table.checkConstraints().getFirst().name());
        assertEquals("IX_OrderLine_UserID", table.indexes().getFirst().name());
    }

    @Test
    void reverseGeneratesFromMetadataSnapshot() {
        DatabaseInfo metadata =
                new DatabaseInfo(
                        "PostgreSQL",
                        "16.2",
                        "CamelSchema",
                        List.of(
                                new SchemaInfo(
                                        "catalog_a",
                                        "CamelSchema",
                                        null,
                                        List.of(metadataTable()))));

        DdlReverseGeneratorResult result =
                NeutralDdlUtils.reverseGenerate(ExternalDatabaseKind.POSTGRESQL, metadata);

        assertTrue(result.success());
        assertNotNull(result.database());
        assertTrue(result.ddl().contains("create table \"CamelSchema\".\"OrderLine\""));
        assertTrue(result.ddl().contains("create index \"IX_OrderLine_UserID\""));
    }

    @Test
    void utilsSourceDoesNotImportForbiddenRuntimeDependencies() throws IOException {
        Path sourceRoot =
                Path.of("src/main/java/org/carl/infrastructure/persistence/utils");
        if (!Files.exists(sourceRoot)) {
            sourceRoot =
                    Path.of(
                            "infrastructure-component-persistence-jooq/src/main/java/org/carl/infrastructure/persistence/utils");
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
        assertFalse(source.contains("ertool"));
        assertFalse(source.contains("BusinessError"));
    }

    private Database databaseModel() {
        return new Database(
                null,
                List.of(
                        new Schema(
                                "CamelSchema",
                                null,
                                List.of(
                                        new Table(
                                                "OrderLine",
                                                "order line table",
                                                List.of(
                                                        new Column(
                                                                "ID",
                                                                new ColumnType("bigint", Types.BIGINT, null, null),
                                                                false,
                                                                null,
                                                                false,
                                                                false,
                                                                "primary id"),
                                                        new Column(
                                                                "UserID",
                                                                new ColumnType("int4", Types.INTEGER, 10, null),
                                                                true,
                                                                "0",
                                                                false,
                                                                false,
                                                                "user id")),
                                                new PrimaryKey("PK_OrderLine", List.of("ID")),
                                                List.of(),
                                                List.of(new Index("IX_OrderLine_UserID", false, List.of("UserID"))),
                                                List.of(new CheckConstraint("CK_OrderLine_ID", "\"ID\" > 0")))))));
    }

    private TableInfo metadataTable() {
        return new TableInfo(
                "catalog_a",
                "CamelSchema",
                "OrderLine",
                "TABLE",
                "order line table",
                List.of(
                        new ColumnInfo(
                                "ID",
                                "primary id",
                                new NormalizedColumnType(
                                        "bigint",
                                        Types.BIGINT,
                                        null,
                                        null,
                                        false,
                                        false,
                                        false,
                                        null,
                                        ColumnTypeFamily.INTEGER),
                                1,
                                false,
                                false,
                                false,
                                null),
                        new ColumnInfo(
                                "UserID",
                                "user id",
                                new NormalizedColumnType(
                                        "int4",
                                        Types.INTEGER,
                                        10,
                                        null,
                                        true,
                                        false,
                                        false,
                                        "0",
                                        ColumnTypeFamily.INTEGER),
                                2,
                                true,
                                false,
                                false,
                                "0")),
                new PrimaryKeyInfo("PK_OrderLine", List.of("ID")),
                List.of(),
                List.of(new IndexInfo("IX_OrderLine_UserID", false, List.of(new IndexColumnInfo("UserID", 1, "A", null)))),
                List.of());
    }
}
