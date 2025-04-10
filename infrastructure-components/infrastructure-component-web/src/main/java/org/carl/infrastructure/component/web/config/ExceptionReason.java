package org.carl.infrastructure.component.web.config;

public interface ExceptionReason {
    String getReason();

    String getErrorType();

    int getCode();

    static ExceptionReason create(String reason, String errorType, int code) {
        return new ExceptionReason() {
            @Override
            public String getReason() {
                return reason;
            }

            @Override
            public String getErrorType() {
                return errorType;
            }

            @Override
            public int getCode() {
                return code;
            }

            @Override
            public String toString() {
                return getReason();
            }
        };
    }
}
