/*
 * This file is generated by jOOQ.
 */
package org.carl.generated.tables;

import java.time.OffsetDateTime;
import java.util.Collection;
import org.carl.generated.Keys;
import org.carl.generated.Public;
import org.carl.generated.tables.records.ImChannelRecord;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Identity;
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
public class ImChannel extends TableImpl<ImChannelRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.im_channel</code>
     */
    public static final ImChannel IM_CHANNEL = new ImChannel();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ImChannelRecord> getRecordType() {
        return ImChannelRecord.class;
    }

    /**
     * The column <code>public.im_channel.channel_id</code>.
     */
    public final TableField<ImChannelRecord, Long> CHANNEL_ID =
            createField(
                    DSL.name("channel_id"),
                    SQLDataType.BIGINT.nullable(false).identity(true),
                    this,
                    "");

    /**
     * The column <code>public.im_channel.created_at</code>.
     */
    public final TableField<ImChannelRecord, OffsetDateTime> CREATED_AT =
            createField(
                    DSL.name("created_at"),
                    SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false),
                    this,
                    "");

    /**
     * The column <code>public.im_channel.updated_at</code>.
     */
    public final TableField<ImChannelRecord, OffsetDateTime> UPDATED_AT =
            createField(
                    DSL.name("updated_at"),
                    SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false),
                    this,
                    "");

    /**
     * The column <code>public.im_channel.type</code>.
     */
    public final TableField<ImChannelRecord, String> TYPE =
            createField(DSL.name("type"), SQLDataType.VARCHAR.nullable(false), this, "");

    private ImChannel(Name alias, Table<ImChannelRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private ImChannel(
            Name alias, Table<ImChannelRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.im_channel</code> table reference
     */
    public ImChannel(String alias) {
        this(DSL.name(alias), IM_CHANNEL);
    }

    /**
     * Create an aliased <code>public.im_channel</code> table reference
     */
    public ImChannel(Name alias) {
        this(alias, IM_CHANNEL);
    }

    /**
     * Create a <code>public.im_channel</code> table reference
     */
    public ImChannel() {
        this(DSL.name("im_channel"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public Identity<ImChannelRecord, Long> getIdentity() {
        return (Identity<ImChannelRecord, Long>) super.getIdentity();
    }

    @Override
    public UniqueKey<ImChannelRecord> getPrimaryKey() {
        return Keys.IM_CHANNEL_PKEY;
    }

    @Override
    public ImChannel as(String alias) {
        return new ImChannel(DSL.name(alias), this);
    }

    @Override
    public ImChannel as(Name alias) {
        return new ImChannel(alias, this);
    }

    @Override
    public ImChannel as(Table<?> alias) {
        return new ImChannel(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ImChannel rename(String name) {
        return new ImChannel(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ImChannel rename(Name name) {
        return new ImChannel(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ImChannel rename(Table<?> name) {
        return new ImChannel(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannel where(Condition condition) {
        return new ImChannel(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannel where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannel where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannel where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannel where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannel where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannel where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ImChannel where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannel whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ImChannel whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
