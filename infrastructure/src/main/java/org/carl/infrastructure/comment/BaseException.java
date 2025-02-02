package org.carl.infrastructure.comment;

public class BaseException extends RuntimeException {
    private int errorCode;
    private Long traceId;

    public BaseException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException(StatusType statusType, Long traceId) {
        super(statusType.getReasonPhrase());
        this.traceId = traceId;
        this.errorCode = statusType.getStatusCode();
    }

    public BaseException setErrorCode(int errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
