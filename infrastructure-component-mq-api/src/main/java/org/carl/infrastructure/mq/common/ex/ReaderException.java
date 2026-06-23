package org.carl.infrastructure.mq.common.ex;

/** Reader 异常 */
public class ReaderException extends Exception {
    public ReaderException(String message) {
        super(message);
    }

    public ReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReaderException(Throwable cause) {
        super(cause);
    }
}
