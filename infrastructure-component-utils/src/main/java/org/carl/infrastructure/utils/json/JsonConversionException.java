package org.carl.infrastructure.utils.json;

import java.io.Serial;

public class JsonConversionException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public JsonConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
