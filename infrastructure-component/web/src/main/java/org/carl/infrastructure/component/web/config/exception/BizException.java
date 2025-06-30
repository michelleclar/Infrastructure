package org.carl.infrastructure.component.web.config.exception;

import java.io.Serial;
import java.util.Objects;

public class BizException extends BaseException {

    @Serial private static final long serialVersionUID = 1L;

    public static final String DEFAULT_BIZ_ERR_CODE = "BIZ_ERROR";

    public BizException(ExceptionReason reason) {
        super(reason);
    }

    public BizException(ExceptionReason reason, Throwable cause) {
        super(reason, cause);
    }

    public static ExceptionReason biz(String reason, String errorType, String scenario) {
        return new ExceptionReason() {
            @Override
            public String getReason() {
                return reason;
            }

            @Override
            public String getErrorType() {
                return Objects.requireNonNullElse(errorType, DEFAULT_BIZ_ERR_CODE);
            }

            @Override
            public int getCode() {
                return -1;
            }

            public String getScenario() {
                return scenario;
            }

            @Override
            public String toString() {
                return getReason();
            }
        };
    }

    public static BizException biz(String reason, String errorType, String scenario, int code) {
        ExceptionReason exceptionReason =
                new ExceptionReason() {
                    @Override
                    public String getReason() {
                        return reason;
                    }

                    @Override
                    public String getErrorType() {
                        return Objects.requireNonNullElse(errorType, DEFAULT_BIZ_ERR_CODE);
                    }

                    @Override
                    public int getCode() {
                        return code;
                    }

                    public String getScenario() {
                        return scenario;
                    }

                    @Override
                    public String toString() {
                        return getReason();
                    }
                };
        return new BizException(exceptionReason);
    }

    public static BizException biz(String reason, int code) {
        ExceptionReason exceptionReason =
                new ExceptionReason() {
                    @Override
                    public String getReason() {
                        return reason;
                    }

                    @Override
                    public String getErrorType() {
                        return DEFAULT_BIZ_ERR_CODE;
                    }

                    @Override
                    public int getCode() {
                        return code;
                    }

                    public String getScenario() {
                        return DEFAULT_BIZ_ERR_CODE;
                    }

                    @Override
                    public String toString() {
                        return getReason();
                    }
                };

        return new BizException(exceptionReason);
    }

    public static BizException biz(String reason) {
        ExceptionReason exceptionReason =
                new ExceptionReason() {
                    @Override
                    public String getReason() {
                        return reason;
                    }

                    @Override
                    public String getErrorType() {
                        return DEFAULT_BIZ_ERR_CODE;
                    }

                    @Override
                    public int getCode() {
                        return -1;
                    }

                    public String getScenario() {
                        return DEFAULT_BIZ_ERR_CODE;
                    }

                    @Override
                    public String toString() {
                        return getReason();
                    }
                };

        return new BizException(exceptionReason);
    }
}
