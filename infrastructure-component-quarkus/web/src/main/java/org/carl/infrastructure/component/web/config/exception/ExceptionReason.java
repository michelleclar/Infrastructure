package org.carl.infrastructure.component.web.config.exception;

public interface ExceptionReason {
    /**
     * error reason
     *
     * @return reason
     */
    String getReason();

    /**
     * error scenario
     *
     * @return scenario
     */
    String getScenario();

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
}
