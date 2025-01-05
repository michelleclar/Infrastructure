package org.carl.infrastructure.comment;

public class BaseException extends RuntimeException {
    private int errorCode;

    public BaseException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException setErrorCode(int errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
