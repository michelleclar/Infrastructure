package org.carl.infrastructure.component.web.config.exception;

import java.io.Serial;
import org.carl.infrastructure.component.web.config.ExceptionReason;

public class SysException extends BaseException {
    @Serial private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERR_CODE = "SYS_ERROR";

    public SysException(ExceptionReason reason, Throwable cause) {
        super(reason, DEFAULT_ERR_CODE, cause);
    }

    public SysException(ExceptionReason reason) {
        super(reason, DEFAULT_ERR_CODE);
    }
}
