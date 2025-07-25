package org.carl.infrastructure.pulsar.common.ex;

/**
 * 生产者异常类
 *
 * <p>封装生产者操作过程中可能出现的各种异常
 */
public class ProducerException extends Exception {
    public ProducerException(String message) {
        super(message);
    }

    public ProducerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProducerException(Throwable cause) {
        super(cause);
    }
}
