package org.carl.infrastructure.component.web.config.exception;

import java.util.Objects;

public interface ExceptionReason {

    String DEFAULT_SYS_ERR_CODE = "SYS_ERROR";

    String DEFAULT_BIZ_ERR_CODE = "BIZ_ERROR";

    /**
     * error reason
     *
     * @return reason
     */
    String getReason();

    /**
     * error type
     *
     * <p>if is biz exception use s
     *
     * @return error type
     */
    String getErrorType();

    /**
     * http err code
     *
     * @return err code
     */
    int getCode();

    /**
     * sys exception
     *
     * @param reason reason
     * @return exception
     */
    static ExceptionReason sys(String reason) {
        return new ExceptionReason() {
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
            public String toString() {
                return getReason();
            }
        };
    }

    static ExceptionReason biz(String reason) {
        return new ExceptionReason() {
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
    }

    static ExceptionReason biz(String reason, String errorType, String scenario) {
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

    static ExceptionReason biz(String reason, String errorType, String scenario, int code) {
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
    }
}
