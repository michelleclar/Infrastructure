package org.carl.infrastructure.utils.crypto;

import java.io.Serial;

public class CryptoException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
