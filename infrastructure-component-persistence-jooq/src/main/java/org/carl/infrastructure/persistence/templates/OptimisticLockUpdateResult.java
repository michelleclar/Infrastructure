package org.carl.infrastructure.persistence.templates;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Neutral result wrapper for optimistic-lock updates.
 */
public record OptimisticLockUpdateResult(int affectedRows) {

    public OptimisticLockUpdateResult {
        if (affectedRows < 0) {
            throw new IllegalArgumentException("affectedRows must not be negative");
        }
    }

    /**
     * @return {@code true} when the update changed at least one row
     */
    public boolean updated() {
        return affectedRows > 0;
    }

    /**
     * @return {@code true} when the update changed no rows
     */
    public boolean conflict() {
        return affectedRows == 0;
    }

    /**
     * Lets applications map a zero-row update to their own exception type.
     *
     * @param exceptionSupplier exception supplier used only when {@link #conflict()} is true
     */
    public void throwIfConflict(Supplier<? extends RuntimeException> exceptionSupplier) {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier");
        if (conflict()) {
            throw Objects.requireNonNull(exceptionSupplier.get(), "exception");
        }
    }
}
