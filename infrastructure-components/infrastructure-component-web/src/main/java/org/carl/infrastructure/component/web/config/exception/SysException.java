package org.carl.infrastructure.component.web.config.exception;

import java.io.Serial;

public class SysException extends BaseException {
    @Serial private static final long serialVersionUID = 1L;

    public static final String DEFAULT_SYS_ERR_CODE = "SYS_ERROR";

    public SysException(ExceptionReason reason, Throwable cause) {
        super(reason, cause);
    }

    public SysException(ExceptionReason reason) {
        super(reason);
    }

    public static SysException sys(String reason) {
        ExceptionReason exceptionReason =
                new ExceptionReason() {
                    @Override
                    public String getReason() {
                        return reason;
                    }

                    @Override
                    public String getErrorType() {
                        return DEFAULT_SYS_ERR_CODE;
                    }

                    @Override
                    public int getCode() {
                        return 500;
                    }

                    @Override
                    public String getScenario() {
                        return DEFAULT_SYS_ERR_CODE;
                    }

                    @Override
                    public String toString() {
                        return getReason();
                    }
                };
        return new SysException(exceptionReason);
    }

    public static SysException sys(String reason, int code) {
        ExceptionReason exceptionReason =
                new ExceptionReason() {
                    @Override
                    public String getReason() {
                        return reason;
                    }

                    @Override
                    public String getErrorType() {
                        return DEFAULT_SYS_ERR_CODE;
                    }

                    @Override
                    public int getCode() {
                        return code;
                    }

                    @Override
                    public String getScenario() {
                        return DEFAULT_SYS_ERR_CODE;
                    }

                    @Override
                    public String toString() {
                        return getReason();
                    }
                };
        return new SysException(exceptionReason);
    }
}
