package org.carl.infrastructure.component.web.config.exception;

import jakarta.ws.rs.core.Response;
import java.io.Serial;

public class BaseException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
    private final ExceptionReason reason;

    public BaseException(ExceptionReason reason) {
        super(reason.toString());
        this.reason = reason;
    }

    public BaseException(ExceptionReason reason, Throwable cause) {
        super(reason.toString(), cause);
        this.reason = reason;
    }

    public ExceptionReason getReason() {
        return this.reason;
    }

    public Response toResponse() {
        return Response.status(getReason().getCode(), getReason().getReason())
                .entity(getReason())
                .build();
    }
}
