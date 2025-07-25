package org.carl.infrastructure.pulsar.common.ex;

/** 消费者异常 */
public class ConsumerException extends Exception {
    public ConsumerException(String message) {
        super(message);
    }

    public ConsumerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConsumerException(Throwable cause) {
        super(cause);
    }
}
