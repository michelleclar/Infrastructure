package org.carl.infrastructure.mq.common.ex;

/** 客户端异常 */
public class MQClientException extends Exception {
    public MQClientException(String message) {
        super(message);
    }

    public MQClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public MQClientException(Throwable cause) {
        super(cause);
    }
}
