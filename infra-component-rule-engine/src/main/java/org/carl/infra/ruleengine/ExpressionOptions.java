package org.carl.infra.ruleengine;

/** Immutable execution settings; timeout is a cooperative QLExpress timeout in milliseconds. */
public record ExpressionOptions(long timeoutMillis, boolean precise) {
    public static final ExpressionOptions DEFAULT = new ExpressionOptions(1000L, false);

    public ExpressionOptions {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
    }
}
