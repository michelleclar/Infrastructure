package org.carl.infrastructure.persistence.metadata;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataModelTest {

    @Test
    void tableColumnLookupSupportsUnquotedNames() {
        DBColumn column = new DBColumn();
        column.setName("username");

        DBTable table = new DBTable().setSchema("public").setTableName("user_account");
        table.addColumn(column);

        assertTrue(table.contains("username"));
        assertSame(column, table.getColumn("USERNAME"));
    }

    @Test
    void tableColumnLookupPreservesQuotedIdentifierSemantics() {
        DBColumn lower = new DBColumn();
        lower.setName("user");
        DBColumn quoted = new DBColumn();
        quoted.setName("User");

        DBTable table = new DBTable().setSchema("public").setTableName("user_account");
        table.addColumn(lower).addColumn(quoted);

        assertSame(lower, table.getColumn("User"));
        assertSame(quoted, table.getColumn("\"User\""));
    }

    @Test
    void indexColumnsKeepDeclaredOrder() {
        DBIndex index = new DBIndex().setName("idx_name");
        index.addColumn(new DBIndexColumn("second_column", 2, "D"));
        index.addColumn(new DBIndexColumn("first_column", 1, "A"));

        assertEquals(List.of("first_column", "second_column"), index.getColumns());
        assertEquals("A", index.getColumnDetails().getFirst().getSortOrder());
        assertTrue(index.isMulti());
    }

    @Test
    void indexColumnsCanRepresentExpressionsAndIncludedColumns() {
        DBIndex index = new DBIndex().setName("idx_expr");
        index.addColumn(new DBIndexColumn(null, 1, null, "lower(name)", false));
        index.addColumn(new DBIndexColumn("tenant_id", 2, "A", "tenant_id", true));

        assertEquals(List.of("lower(name)"), index.getColumns());
        assertTrue(index.getColumnDetails().get(1).isIncluded());
    }

    @Test
    void columnKeepsRawDefaultValueAlongsideParsedValue() {
        DBColumn boolColumn = new DBColumn();
        boolColumn.setType("bool");
        boolColumn.setRawDefaultValue("true");

        DBColumn varcharColumn = new DBColumn();
        varcharColumn.setType("varchar");
        varcharColumn.setRawDefaultValue("'hello'");

        assertEquals("true", boolColumn.getRawDefaultValue());
        assertEquals(true, boolColumn.getDefaultValue());
        assertEquals("hello", varcharColumn.getDefaultValue());
    }

    @Test
    void infoAndSchemaKeepTablesAsStructuredSnapshots() {
        DBTable table = new DBTable().setSchema("public").setTableName("orders");
        DBInfo info = new DBInfo("PostgreSQL", "16").setDialect("PostgreSQL");
        info.getOrAddSchema("public").setOwner("postgres").addTable(table);

        assertEquals("PostgreSQL", info.getDialect());
        assertSame(table, info.getSchema("PUBLIC").getTable("ORDERS"));
        assertEquals(Map.of("orders", table), info.getSchema("public").getTables());
        assertEquals("postgres", info.getSchema("public").getOwner());
    }

    @Test
    void tableKeepsCheckConstraintsAndOwner() {
        DBTable table = new DBTable().setSchema("public").setTableName("orders").setOwner("app");
        table.addCheckConstraint(new DBCheckConstraint("orders_status_check", "CHECK (status > 0)"));

        assertEquals("app", table.getOwner());
        assertEquals(1, table.getCheckConstraints().size());
        assertEquals("orders_status_check", table.getCheckConstraints().getFirst().getName());
    }
}
