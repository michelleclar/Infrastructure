package org.carl.infrastructure.pulsar.builder;

import org.carl.infrastructure.pulsar.common.ex.ConsumerException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 消息消费者接口 提供统一的消息消费抽象，支持 Pulsar、Kafka 等不同消息中间件
 *
 * @param <T> 消息类型
 */
public interface IConsumer<T> {

    // ===== 基础消费功能 =====

    /**
     * 同步接收消息（阻塞）
     *
     * @return 接收到的消息
     * @throws ConsumerException 接收异常
     */
    MessageBuilder.Message<T> receive() throws ConsumerException;

    /**
     * 同步接收消息，带超时
     *
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 接收到的消息，超时返回 null
     * @throws ConsumerException 接收异常
     */
    MessageBuilder.Message<T> receive(int timeout, TimeUnit unit) throws ConsumerException;

    /**
     * 异步接收消息
     *
     * @return 消息的 Future
     */
    CompletableFuture<MessageBuilder.Message<T>> receiveAsync() throws ConsumerException;

    // ===== 批量消费功能 =====

    // ===== 消息确认功能 =====

    /**
     * 确认消息
     *
     * @param message 要确认的消息
     * @throws ConsumerException 确认异常
     */
    void acknowledge(MessageBuilder.Message<T> message) throws ConsumerException;

    /**
     * 异步确认消息
     *
     * @param message 要确认的消息
     * @return 确认完成的 Future
     */
    CompletableFuture<Void> acknowledgeAsync(MessageBuilder.Message<T> message);

    /**
     * 累积确认消息（确认到指定消息为止的所有消息）
     *
     * @param message 累积确认的最后一条消息
     * @throws ConsumerException 确认异常
     */
    void acknowledgeCumulative(MessageBuilder.Message<T> message) throws ConsumerException;

    /**
     * 异步累积确认消息
     *
     * @param message 累积确认的最后一条消息
     * @return 确认完成的 Future
     */
    CompletableFuture<Void> acknowledgeCumulativeAsync(MessageBuilder.Message<T> message);

    /**
     * 否定确认消息（触发重新投递）
     *
     * @param message 要否定确认的消息
     */
    void negativeAcknowledge(MessageBuilder.Message<T> message);

    // ===== 消费者控制功能 =====

    /** 暂停消费 */
    void pause();

    /** 恢复消费 */
    void resume();

    /**
     * 重置消费位置到指定时间戳
     *
     * @param timestamp 时间戳
     * @throws ConsumerException 重置异常
     */
    void seek(long timestamp) throws ConsumerException;

    /**
     * 重置消费位置到指定消息ID
     *
     * @param messageId 消息ID
     * @throws ConsumerException 重置异常
     */
    void seek(String messageId) throws ConsumerException;

    // ===== 消费者信息功能 =====

    /**
     * 获取消费者统计信息
     *
     * @return 统计信息
     */
    @Deprecated
    ConsumerStats getStats();

    /**
     * 检查消费者是否已连接
     *
     * @return 是否已连接
     */
    boolean isConnected();
    /**
     * 关闭消费者
     *
     * @throws ConsumerException 关闭异常
     */
    void close() throws ConsumerException;

    /**
     * 异步关闭消费者
     *
     * @return 关闭完成的 Future
     */
    CompletableFuture<Void> closeAsync();

    // ===== 监听器接口 =====

    /**
     * 消息监听器接口
     *
     * @param <T> 消息类型
     */
    interface MessageListener<T> {
        /**
         * 接收到消息时的回调
         *
         * @param consumer 消费者实例
         * @param message 接收到的消息
         */
        void received(IConsumer<T> consumer, MessageBuilder.Message<T> message)
                throws ConsumerException;

        /**
         * 消费异常时的回调
         *
         * @param consumer 消费者实例
         * @param exception 异常信息
         */
        default void onException(IConsumer<T> consumer, Throwable exception) {
            // 默认实现：记录日志
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

    // ===== 枚举和统计类 =====

    /** 订阅类型枚举 */
    enum SubscriptionType {
        EXCLUSIVE, // 独占订阅
        SHARED, // 共享订阅
        FAILOVER, // 故障转移订阅
        KEY_SHARED // 按键共享订阅
    }

    enum SubscriptionMode {
        // Make the subscription to be backed by a durable cursor that will retain messages and
        // persist the current
        // position
        Durable,

        // Lightweight subscription mode that doesn't have a durable cursor associated
        NonDurable
    }

    enum SubscriptionInitialPosition {
        /**
         * The latest position which means the start consuming position will be the last message.
         */
        Latest(0),

        /**
         * The earliest position which means the start consuming position will be the first message.
         */
        Earliest(1);

        private final int value;

        SubscriptionInitialPosition(int value) {
            this.value = value;
        }

        public final int getValue() {
            return value;
        }
    }

    /** 消费者统计信息 */
    interface ConsumerStats {
        /**
         * 获取接收消息总数
         *
         * @return 接收消息总数
         */
        long getTotalReceivedMessages();

        /**
         * 获取接收字节总数
         *
         * @return 接收字节总数
         */
        long getTotalReceivedBytes();

        /**
         * 获取确认消息总数
         *
         * @return 确认消息总数
         */
        long getTotalAckedMessages();

        /**
         * 获取否定确认消息总数
         *
         * @return 否定确认消息总数
         */
        long getTotalNackedMessages();

        /**
         * 获取平均消费延迟（毫秒）
         *
         * @return 平均消费延迟
         */
        double getAverageConsumeLatency();

        /**
         * 获取当前未确认消息数
         *
         * @return 未确认消息数
         */
        int getUnackedMessages();

        /**
         * 获取最后接收时间
         *
         * @return 最后接收时间戳
         */
        long getLastReceivedTimestamp();

        /**
         * 获取最后确认时间
         *
         * @return 最后确认时间戳
         */
        long getLastAckedTimestamp();
    }
}
