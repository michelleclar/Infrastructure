package org.carl.infrastructure.mq.consumer;

import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.model.Message;

/**
 * 消息监听器接口
 *
 * @param <T> 消息类型
 */
public interface MessageListener<T> {
    /**
     * 接收到消息时的回调
     *
     * @param consumer 消费者实例
     * @param message 接收到的消息
     */
    void received(IConsumer<T> consumer, Message<T> message) throws ConsumerException;

    /**
     * 消费异常时的回调
     *
     * @param consumer 消费者实例
     * @param exception 异常信息
     */
    default void onException(IConsumer<T> consumer, Throwable exception) throws ConsumerException {
        throw new ConsumerException(exception);
    }

    /**
     * Get the notification when a topic is terminated.
     *
     * @param consumer the Consumer object associated with the terminated topic
     */
    default void reachedEndOfTopic(IConsumer<T> consumer) {
        // By default ignore the notification
    }
}
