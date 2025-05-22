package org.carl.infrastructure.component.web.config.exception;

import java.io.Serial;

public class SysException extends BaseException {
    @Serial private static final long serialVersionUID = 1L;

    public SysException(ExceptionReason reason, Throwable cause) {
        super(reason, cause);
    }

    public SysException(ExceptionReason reason) {
        super(reason);
    }
}
