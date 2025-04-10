package org.carl.infrastructure.component.web.config.exception;

import org.carl.infrastructure.component.web.config.ExceptionReason;

import java.io.Serial;

public class BizException extends BaseException {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERR_CODE = "BIZ_ERROR";

    public BizException(ExceptionReason reason, String errorType) {
        super(reason, errorType);
    }

    public BizException(ExceptionReason reason, String errorType, Throwable cause) {
        super(reason, errorType, cause);
    }
}
