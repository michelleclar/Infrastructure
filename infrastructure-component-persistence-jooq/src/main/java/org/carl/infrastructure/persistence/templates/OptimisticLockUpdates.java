package org.carl.infrastructure.persistence.templates;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.UpdateConditionStep;
import org.jooq.UpdateSetMoreStep;
import org.jooq.UpdateSetStep;
import org.jooq.UpdateWhereStep;

import java.util.Objects;

/**
 * jOOQ helpers for optimistic-lock update statements.
 */
public final class OptimisticLockUpdates {

    private static final int DEFAULT_INCREMENT = 1;

    private OptimisticLockUpdates() {}

    /**
     * Builds {@code versionField = expectedVersion}.
     *
     * @param versionField caller supplied optimistic-lock field
     * @param expectedVersion expected row version
     * @param <T> numeric version type
     * @return version equality condition
     */
    public static <T extends Number> Condition versionMatches(Field<T> versionField, T expectedVersion) {
        return Objects.requireNonNull(versionField, "versionField")
                .eq(Objects.requireNonNull(expectedVersion, "expectedVersion"));
    }

    /**
     * Builds {@code versionField + 1}.
     *
     * @param versionField caller supplied optimistic-lock field
     * @param <T> numeric version type
     * @return increment expression
     */
    public static <T extends Number> Field<T> incrementedVersion(Field<T> versionField) {
        return incrementedVersion(versionField, DEFAULT_INCREMENT);
    }

    /**
     * Builds {@code versionField + increment}.
     *
     * @param versionField caller supplied optimistic-lock field
     * @param increment numeric increment
     * @param <T> numeric version type
     * @return increment expression
     */
    public static <T extends Number> Field<T> incrementedVersion(Field<T> versionField, Number increment) {
        return Objects.requireNonNull(versionField, "versionField")
                .plus(Objects.requireNonNull(increment, "increment"));
    }

    /**
     * Adds {@code SET versionField = versionField + 1} to an update.
     *
     * @param update jOOQ update set step
     * @param versionField caller supplied optimistic-lock field
     * @param <R> table record type
     * @param <T> numeric version type
     * @return update step with the version increment
     */
    public static <R extends Record, T extends Number> UpdateSetMoreStep<R> withVersionIncrement(
            UpdateSetStep<R> update,
            Field<T> versionField) {
        return withVersionIncrement(update, versionField, DEFAULT_INCREMENT);
    }

    /**
     * Adds {@code SET versionField = versionField + increment} to an update.
     *
     * @param update jOOQ update set step
     * @param versionField caller supplied optimistic-lock field
     * @param increment numeric increment
     * @param <R> table record type
     * @param <T> numeric version type
     * @return update step with the version increment
     */
    public static <R extends Record, T extends Number> UpdateSetMoreStep<R> withVersionIncrement(
            UpdateSetStep<R> update,
            Field<T> versionField,
            Number increment) {
        Objects.requireNonNull(versionField, "versionField");
        return Objects.requireNonNull(update, "update")
                .set(versionField, incrementedVersion(versionField, increment));
    }

    /**
     * Applies a base filter and {@code versionField = expectedVersion} to an update.
     *
     * @param update jOOQ update where step
     * @param versionField caller supplied optimistic-lock field
     * @param expectedVersion expected row version
     * @param condition base filter supplied by the caller
     * @param <R> table record type
     * @param <T> numeric version type
     * @return update step with the version condition
     */
    public static <R extends Record, T extends Number> UpdateConditionStep<R> whereVersionMatches(
            UpdateWhereStep<R> update,
            Field<T> versionField,
            T expectedVersion,
            Condition condition) {
        return Objects.requireNonNull(update, "update")
                .where(Objects.requireNonNull(condition, "condition"))
                .and(versionMatches(versionField, expectedVersion));
    }

    /**
     * Adds {@code SET versionField = versionField + 1} and a version check to an update.
     *
     * @param update update step after caller supplied field assignments
     * @param versionField caller supplied optimistic-lock field
     * @param expectedVersion expected row version
     * @param condition base filter supplied by the caller
     * @param <R> table record type
     * @param <T> numeric version type
     * @return executable update step
     */
    public static <R extends Record, T extends Number> UpdateConditionStep<R> withVersionCheckAndIncrement(
            UpdateSetMoreStep<R> update,
            Field<T> versionField,
            T expectedVersion,
            Condition condition) {
        return withVersionCheckAndIncrement(update, versionField, expectedVersion, DEFAULT_INCREMENT, condition);
    }

    /**
     * Adds {@code SET versionField = versionField + increment} and a version check to an update.
     *
     * @param update update step after caller supplied field assignments
     * @param versionField caller supplied optimistic-lock field
     * @param expectedVersion expected row version
     * @param increment numeric increment
     * @param condition base filter supplied by the caller
     * @param <R> table record type
     * @param <T> numeric version type
     * @return executable update step
     */
    public static <R extends Record, T extends Number> UpdateConditionStep<R> withVersionCheckAndIncrement(
            UpdateSetMoreStep<R> update,
            Field<T> versionField,
            T expectedVersion,
            Number increment,
            Condition condition) {
        UpdateSetMoreStep<R> incrementedUpdate = Objects.requireNonNull(update, "update")
                .set(Objects.requireNonNull(versionField, "versionField"), incrementedVersion(versionField, increment));
        return whereVersionMatches(incrementedUpdate, versionField, expectedVersion, condition);
    }

    /**
     * Executes an optimistic-lock update and converts the affected row count into a neutral result.
     *
     * @param update executable jOOQ query
     * @return neutral optimistic-lock update result
     */
    public static OptimisticLockUpdateResult execute(Query update) {
        return fromAffectedRows(Objects.requireNonNull(update, "update").execute());
    }

    /**
     * Converts an affected row count into a neutral optimistic-lock update result.
     *
     * @param affectedRows jOOQ affected row count
     * @return neutral optimistic-lock update result
     */
    public static OptimisticLockUpdateResult fromAffectedRows(int affectedRows) {
        return new OptimisticLockUpdateResult(affectedRows);
    }
}
