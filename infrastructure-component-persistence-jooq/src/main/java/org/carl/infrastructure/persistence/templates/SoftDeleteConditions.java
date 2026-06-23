package org.carl.infrastructure.persistence.templates;

import org.jooq.Condition;
import org.jooq.Field;

import java.util.Objects;

/**
 * jOOQ condition helpers for tables that represent soft delete state with a nullable field.
 */
public final class SoftDeleteConditions {

    private SoftDeleteConditions() {}

    /**
     * Builds the neutral soft-delete condition used by query and update filters.
     *
     * @param deletedAtField caller supplied soft-delete field
     * @return {@code deletedAtField IS NULL}
     */
    public static Condition notDeleted(Field<?> deletedAtField) {
        return Objects.requireNonNull(deletedAtField, "deletedAtField").isNull();
    }

    /**
     * Prepends {@code deletedAtField IS NULL} to additional filters with {@code AND}.
     *
     * @param deletedAtField caller supplied soft-delete field
     * @param condition additional filter
     * @return {@code deletedAtField IS NULL AND condition}
     */
    public static Condition notDeletedAnd(Field<?> deletedAtField, Condition condition) {
        return notDeleted(deletedAtField).and(Objects.requireNonNull(condition, "condition"));
    }

    /**
     * Prepends {@code deletedAtField IS NULL} to zero or more filters with {@code AND}.
     *
     * @param deletedAtField caller supplied soft-delete field
     * @param conditions additional filters
     * @return combined condition
     */
    public static Condition notDeletedAnd(Field<?> deletedAtField, Condition... conditions) {
        Objects.requireNonNull(conditions, "conditions");
        Condition result = notDeleted(deletedAtField);
        for (Condition condition : conditions) {
            result = result.and(Objects.requireNonNull(condition, "condition"));
        }
        return result;
    }
}
