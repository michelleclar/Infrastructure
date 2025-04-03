/*
 * This file is generated by jOOQ.
 */
package org.carl.generated.tables;

import java.util.Collection;
import org.carl.generated.Keys;
import org.carl.generated.Public;
import org.carl.generated.tables.records.ImChannelViewRecord;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class ImChannelView extends TableImpl<ImChannelViewRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.im_channel_view</code>
     */
    public static final ImChannelView IM_CHANNEL_VIEW = new ImChannelView();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ImChannelViewRecord> getRecordType() {
        return ImChannelViewRecord.class;
    }

    /**
     * The column <code>public.im_channel_view.channel_view_id</code>.
     */
    public final TableField<ImChannelViewRecord, Long> CHANNEL_VIEW_ID =
            createField(
                    DSL.name("channel_view_id"),
                    SQLDataType.BIGINT.nullable(false).identity(true),
                    this,
                    "");

    /**
     * The column <code>public.im_channel_view.channel_id</code>. 频道id
     */
    public final TableField<ImChannelViewRecord, Long> CHANNEL_ID =
            createField(DSL.name("channel_id"), SQLDataType.BIGINT.nullable(false), this, "频道id");

    /**
     * The column <code>public.im_channel_view.channel_view</code>.
     */
    public final TableField<ImChannelViewRecord, JSONB> CHANNEL_VIEW =
            createField(DSL.name("channel_view"), SQLDataType.JSONB.nullable(false), this, "");

    private ImChannelView(Name alias, Table<ImChannelViewRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private ImChannelView(
            Name alias,
            Table<ImChannelViewRecord> aliased,
            Field<?>[] parameters,
            Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.im_channel_view</code> table reference
     */
    public ImChannelView(String alias) {
        this(DSL.name(alias), IM_CHANNEL_VIEW);
    }

    /**
     * Create an aliased <code>public.im_channel_view</code> table reference
     */
    public ImChannelView(Name alias) {
        this(alias, IM_CHANNEL_VIEW);
    }

    /**
     * Create a <code>public.im_channel_view</code> table reference
     */
    public ImChannelView() {
        this(DSL.name("im_channel_view"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public Identity<ImChannelViewRecord, Long> getIdentity() {
        return (Identity<ImChannelViewRecord, Long>) super.getIdentity();
    }

    @Override
    public UniqueKey<ImChannelViewRecord> getPrimaryKey() {
        return Keys.IM_CHANNEL_VIEW_PKEY;
    }

    @Override
    public ImChannelView as(String alias) {
        return new ImChannelView(DSL.name(alias), this);
    }

    @Override
    public ImChannelView as(Name alias) {
        return new ImChannelView(alias, this);
    }

    @Override
    public ImChannelView as(Table<?> alias) {
        return new ImChannelView(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ImChannelView rename(String name) {
        return new ImChannelView(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ImChannelView rename(Name name) {
        return new ImChannelView(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ImChannelView rename(Table<?> name) {
        return new ImChannelView(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannelView where(Condition condition) {
        return new ImChannelView(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannelView where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannelView where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannelView where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannelView where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannelView where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannelView where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannelView where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannelView whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannelView whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
