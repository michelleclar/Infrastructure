package org.carl.infra.discover;

/** Indicates that a dynamic configuration document is not safe to activate. */
public class DynamicConfigValidationException extends RuntimeException {

    public DynamicConfigValidationException(String message) {
        super(message);
    }

    public DynamicConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
