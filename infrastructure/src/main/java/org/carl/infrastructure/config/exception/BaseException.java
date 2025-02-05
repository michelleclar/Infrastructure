package org.carl.infrastructure.config.exception;

import jakarta.ws.rs.core.Response;
import org.carl.infrastructure.comment.StatusType;

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

    public Response getErrorResponse() {
        return Response.status(this.errorCode)
                .entity(new ErrorPair(traceId, this.getMessage()))
                .build();
    }

    public int getErrorCode() {
        return errorCode;
    }

    record ErrorPair(Long traceId, String message) {}
}
