package org.carl.infrastructure.mq.common.ex;

/** Source admin operation exception. */
public class SourceAdminException extends Exception {
    public SourceAdminException(String message) {
        super(message);
    }

    public SourceAdminException(String message, Throwable cause) {
        super(message, cause);
    }

    public SourceAdminException(Throwable cause) {
        super(cause);
    }
}
