package org.carl.infrastructure.approval.core;

import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public final class ApprovalTables {
    public static final Table<?> APPROVAL_INSTANCE =
            DSL.table(DSL.name("approval", "approval_instance"));
    public static final Field<Long> INSTANCE_ID =
            DSL.field(DSL.name("approval", "approval_instance", "id"), SQLDataType.BIGINT);
    public static final Field<String> INSTANCE_BIZ_KEY =
            DSL.field(DSL.name("approval", "approval_instance", "biz_key"), SQLDataType.VARCHAR);
    public static final Field<String> INSTANCE_STATUS =
            DSL.field(DSL.name("approval", "approval_instance", "status"), SQLDataType.VARCHAR);
    public static final Field<Integer> INSTANCE_CURRENT_STEP =
            DSL.field(
                    DSL.name("approval", "approval_instance", "current_step"), SQLDataType.INTEGER);
    public static final Field<String> INSTANCE_NODES_JSON =
            DSL.field(DSL.name("approval", "approval_instance", "nodes_json"), SQLDataType.CLOB);
    public static final Field<String> INSTANCE_CREATED_BY =
            DSL.field(DSL.name("approval", "approval_instance", "created_by"), SQLDataType.VARCHAR);
    public static final Field<java.time.LocalDateTime> INSTANCE_CREATED_AT =
            DSL.field(
                    DSL.name("approval", "approval_instance", "created_at"),
                    SQLDataType.LOCALDATETIME);
    public static final Field<java.time.LocalDateTime> INSTANCE_UPDATED_AT =
            DSL.field(
                    DSL.name("approval", "approval_instance", "updated_at"),
                    SQLDataType.LOCALDATETIME);

    public static final Table<?> APPROVAL_TASK = DSL.table(DSL.name("approval", "approval_task"));
    public static final Field<Long> TASK_ID =
            DSL.field(DSL.name("approval", "approval_task", "id"), SQLDataType.BIGINT);
    public static final Field<Long> TASK_INSTANCE_ID =
            DSL.field(DSL.name("approval", "approval_task", "instance_id"), SQLDataType.BIGINT);
    public static final Field<String> TASK_NODE_ID =
            DSL.field(DSL.name("approval", "approval_task", "node_id"), SQLDataType.VARCHAR);
    public static final Field<String> TASK_ASSIGNEE =
            DSL.field(DSL.name("approval", "approval_task", "assignee"), SQLDataType.VARCHAR);
    public static final Field<String> TASK_STATUS =
            DSL.field(DSL.name("approval", "approval_task", "status"), SQLDataType.VARCHAR);
    public static final Field<java.time.LocalDateTime> TASK_CREATED_AT =
            DSL.field(
                    DSL.name("approval", "approval_task", "created_at"), SQLDataType.LOCALDATETIME);
    public static final Field<java.time.LocalDateTime> TASK_UPDATED_AT =
            DSL.field(
                    DSL.name("approval", "approval_task", "updated_at"), SQLDataType.LOCALDATETIME);

    public static final Table<?> APPROVAL_HISTORY =
            DSL.table(DSL.name("approval", "approval_history"));
    public static final Field<Long> HISTORY_ID =
            DSL.field(DSL.name("approval", "approval_history", "id"), SQLDataType.BIGINT);
    public static final Field<Long> HISTORY_INSTANCE_ID =
            DSL.field(DSL.name("approval", "approval_history", "instance_id"), SQLDataType.BIGINT);
    public static final Field<Long> HISTORY_TASK_ID =
            DSL.field(DSL.name("approval", "approval_history", "task_id"), SQLDataType.BIGINT);
    public static final Field<String> HISTORY_ACTION =
            DSL.field(DSL.name("approval", "approval_history", "action"), SQLDataType.VARCHAR);
    public static final Field<String> HISTORY_OPERATOR =
            DSL.field(DSL.name("approval", "approval_history", "operator"), SQLDataType.VARCHAR);
    public static final Field<String> HISTORY_COMMENT =
            DSL.field(DSL.name("approval", "approval_history", "comment"), SQLDataType.VARCHAR);
    public static final Field<java.time.LocalDateTime> HISTORY_CREATED_AT =
            DSL.field(
                    DSL.name("approval", "approval_history", "created_at"),
                    SQLDataType.LOCALDATETIME);

    private ApprovalTables() {}
}
