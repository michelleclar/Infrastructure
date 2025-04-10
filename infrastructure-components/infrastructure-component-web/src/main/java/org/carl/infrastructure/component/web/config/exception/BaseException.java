package org.carl.infrastructure.component.web.config.exception;

import jakarta.ws.rs.core.Response;
import java.io.Serial;
import org.carl.infrastructure.component.web.config.ExceptionReason;

public class BaseException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
    private final String errorType;
    private ExceptionReason reason;

    public BaseException(ExceptionReason reason, String errorType) {
        super(reason.toString());
        this.errorType = errorType;
    }

    public BaseException(ExceptionReason reason, String errorType, Throwable cause) {
        super(reason.toString(), cause);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return this.errorType;
    }

    public ExceptionReason getReason() {
        return this.reason;
    }

    public Response toResponse() {
        return Response.status(getReason().getCode()).type(errorType).entity(getReason()).build();
    }
}
