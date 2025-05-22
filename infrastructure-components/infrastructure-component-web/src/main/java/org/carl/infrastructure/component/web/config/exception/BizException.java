package org.carl.infrastructure.component.web.config.exception;

import java.io.Serial;

public class BizException extends BaseException {

    @Serial private static final long serialVersionUID = 1L;

    public BizException(ExceptionReason reason) {
        super(reason);
    }

    public BizException(ExceptionReason reason, Throwable cause) {
        super(reason, cause);
    }

    public BizException(String reason) {
        super(ExceptionReason.biz(reason));
    }
}
