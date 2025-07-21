package org.carl.infrastructure.pulsar.factory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 消息生产者接口 提供统一的消息发送抽象，支持 Pulsar、Kafka 等不同消息中间件
 *
 * <p>特性：
 *
 * <ul>
 *   <li>使用 MessageBuilder 模式构建消息，支持灵活的消息配置
 *   <li>默认同步发送，提供异步发送选项
 *   <li>支持批量发送、延迟发送、事务发送等高级功能
 *   <li>完整的回调机制和错误处理
 * </ul>
 *
 * @param <T> 消息类型
 */
public interface IProducer<T> extends AutoCloseable {

    // ===== 基础发送功能 =====

    SendResult<T> sendMessage(T value) throws ProducerException;

    SendResult<T> sendMessage(T value, SendCallback<T> callback) throws ProducerException;

    /**
     * 同步发送消息（默认方式）
     *
     * <p>使用 MessageBuilder 构建消息，支持设置分区键、属性、延迟等
     *
     * @param consumer 消息构建器，包含消息内容和发送配置
     * @return 发送结果，包含消息ID、分区信息等
     * @throws Throwable pulsar {@link org.apache.pulsar.client.api.PulsarClientException}
     */
    SendResult<T> sendMessage(T value, Consumer<MessageBuilder<T>> consumer)
            throws ProducerException;

    SendResult<T> sendMessage(
            T value, Consumer<MessageBuilder<T>> consumer, SendCallback<T> callback)
            throws ProducerException;

    /**
     * 异步发送消息，返回 Future
     *
     * <p>非阻塞发送，立即返回 Future 对象
     *
     * @param messageBuilder 消息构建器，包含消息内容和发送配置
     * @return 发送结果的 Future，可用于获取异步发送结果
     */
    CompletableFuture<SendResult<T>> sendMessageAsync(T value) throws ProducerException;

    CompletableFuture<SendResult<T>> sendMessageAsync(T value, SendCallback<T> callback)
            throws ProducerException;

    CompletableFuture<SendResult<T>> sendMessageAsync(T value, Consumer<MessageBuilder<T>> consumer)
            throws ProducerException;

    CompletableFuture<SendResult<T>> sendMessageAsync(
            T value, Consumer<MessageBuilder<T>> consumer, SendCallback<T> callback)
            throws ProducerException;

    // ===== 批量发送功能 =====

    /**
     * 异步批量发送消息
     *
     * <p>批量发送可以提高吞吐量，减少网络开销
     *
     * @param messages 消息构建器列表，每个构建器包含一条消息的配置
     */
    void sendMessages(List<MessageBuilder<T>> messages);

    /**
     * 异步批量发送消息，带回调
     *
     * <p>支持批量发送结果的统一处理，包括全部成功、全部失败、部分成功等情况
     *
     * @param messages 消息构建器列表
     * @param callback 批量发送结果回调
     */
    void sendMessages(List<MessageBuilder<T>> messages, BatchSendCallback<T> callback);

    /**
     * 异步批量发送消息，返回 Future
     *
     * <p>返回包含所有消息发送结果的 Future 列表
     *
     * @param messages 消息构建器列表
     * @return 发送结果列表的 Future，每个结果对应一条消息
     */
    CompletableFuture<List<SendResult<T>>> sendMessagesAsync(List<MessageBuilder<T>> messages);

    // ===== 延迟发送功能 =====

    /**
     * 延迟发送消息
     *
     * <p>消息将在指定延迟时间后才被消费者接收
     *
     * @param message 消息构建器，包含消息内容和配置
     * @param delayMillis 延迟时间（毫秒），消息延迟投递的时间
     */
    void sendDelayedMessage(MessageBuilder<T> message, long delayMillis);

    /**
     * 延迟发送消息，带回调
     *
     * <p>延迟发送消息并通过回调处理发送结果
     *
     * @param message 消息构建器，包含消息内容和配置
     * @param delayMillis 延迟时间（毫秒）
     * @param callback 发送结果回调，处理延迟发送的结果
     */
    void sendDelayedMessage(MessageBuilder<T> message, long delayMillis, SendCallback<T> callback);

    // ===== 事务发送功能 =====

    /**
     * 在事务中同步发送消息
     *
     * <p>消息将作为事务的一部分发送，只有在事务提交时才会被消费者看到
     *
     * @param message 消息构建器，包含消息内容和配置
     * @throws ProducerException 发送异常，包括事务相关异常
     */
    void sendMessageInTransaction(MessageBuilder<T> message) throws ProducerException;

    /**
     * 在事务中同步发送消息
     *
     * <p>消息将作为事务的一部分发送，只有在事务提交时才会被消费者看到
     *
     * @param message 消息构建器，包含消息内容和配置
     * @param transaction 事务对象，用于保证消息发送的事务性
     * @throws ProducerException 发送异常，包括事务相关异常
     */
    void sendMessageInTransaction(MessageBuilder<T> message, Object transaction)
            throws ProducerException;

    /**
     * 在事务中异步发送消息
     *
     * <p>异步发送事务消息，消息将在事务提交时生效
     *
     * @param message 消息构建器，包含消息内容和配置
     * @throws ProducerException 发送异常，包括事务相关异常
     */
    void sendMessageInTransactionAsync(MessageBuilder<T> message) throws ProducerException;

    /**
     * 在事务中异步发送消息
     *
     * <p>异步发送事务消息，消息将在事务提交时生效
     *
     * @param message 消息构建器，包含消息内容和配置
     * @param transaction 事务对象，用于保证消息发送的事务性
     * @throws ProducerException 发送异常，包括事务相关异常
     */
    void sendMessageInTransactionAsync(MessageBuilder<T> message, Object transaction)
            throws ProducerException;

    // ===== 生产者管理功能 =====

    /**
     * 刷新缓冲区，确保所有消息都被发送
     *
     * <p>强制发送所有缓冲区中的消息，通常用于确保消息及时发送
     *
     * @throws ProducerException 刷新异常，如网络异常或生产者已关闭
     */
    void flush() throws ProducerException;

    /**
     * 异步刷新缓冲区
     *
     * <p>非阻塞方式刷新缓冲区，返回 Future 用于监控刷新完成状态
     *
     * @return 刷新完成的 Future，完成时表示所有缓冲消息已发送
     */
    CompletableFuture<Void> flushAsync();

    /**
     * 获取生产者统计信息
     *
     * <p>包含发送消息数、字节数、失败数、延迟等统计数据
     *
     * @return 统计信息，用于监控和调试
     */
    @Deprecated
    ProducerStats getStats();

    /**
     * 检查生产者是否已连接
     *
     * <p>用于健康检查和故障诊断
     *
     * @return 是否已连接到消息中间件
     */
    boolean isConnected();

    /**
     * 获取主题名称
     *
     * <p>返回生产者绑定的主题名称
     *
     * @return 主题名称
     */
    String getTopic();

    /**
     * 获取生产者名称
     *
     * <p>返回生产者的唯一标识名称
     *
     * @return 生产者名称，用于标识和监控
     */
    String getProducerName();

    /**
     * 异步关闭生产者
     *
     * <p>非阻塞方式关闭生产者，返回 Future 用于监控关闭完成状态
     *
     * @return 关闭完成的 Future，完成时表示生产者已完全关闭
     */
    CompletableFuture<Void> closeAsync();

    // ===== 回调接口 =====

    /**
     * 发送结果回调接口
     *
     * <p>用于处理异步发送的结果，提供成功和失败两种回调方法
     */
    interface SendCallback<T> {
        /**
         * 发送成功回调
         *
         * <p>当消息成功发送到消息中间件时调用
         *
         * @param result 发送结果，包含消息ID、分区信息、时间戳等
         */
        void onSuccess(SendResult<T> result);

        /**
         * 发送失败回调
         *
         * <p>当消息发送失败时调用，可用于实现重试逻辑或错误处理
         *
         * @param exception 异常信息，包含失败原因
         */
        void onFailure(Throwable exception);
    }

    /**
     * 批量发送结果回调接口
     *
     * <p>用于处理批量发送的结果，支持全部成功、全部失败、部分成功三种情况
     */
    interface BatchSendCallback<T> {
        /**
         * 批量发送全部成功回调
         *
         * <p>当批量中的所有消息都成功发送时调用
         *
         * @param results 发送结果列表，每个结果对应一条消息
         */
        void onSuccess(List<SendResult<T>> results);

        /**
         * 批量发送全部失败回调
         *
         * <p>当批量发送完全失败时调用（如网络异常、生产者关闭等）
         *
         * @param exception 异常信息，导致批量发送失败的原因
         */
        void onFailure(Throwable exception);

        /**
         * 批量发送部分成功回调
         *
         * <p>当批量中部分消息成功、部分消息失败时调用
         *
         * @param results 发送结果列表，包含成功和失败的结果，可通过 isSuccess() 判断
         */
        void onPartialSuccess(List<SendResult<T>> results);
    }

    // ===== 结果和异常类 =====

    /**
     * 发送结果接口
     *
     * <p>包含消息发送后的详细信息，用于确认发送状态和获取消息元数据
     */
    interface SendResult<T> {
        MessageBuilder.Message<T> getMessage();

        /**
         * 是否发送成功
         *
         * <p>用于判断消息是否成功发送到消息中间件
         *
         * @return true表示发送成功，false表示发送失败
         */
        boolean isSuccess();

        /**
         * 获取错误信息（如果失败）
         *
         * <p>当发送失败时，返回具体的错误描述
         *
         * @return 错误信息，发送成功时为null
         */
        String getErrorMessage();
    }

    /**
     * 生产者统计信息接口
     *
     * <p>提供生产者的运行时统计数据，用于监控和性能分析
     */
    interface ProducerStats {
        /**
         * 获取发送消息总数
         *
         * <p>生产者启动以来成功发送的消息总数
         *
         * @return 发送消息总数
         */
        long getTotalSentMessages();

        /**
         * 获取发送字节总数
         *
         * <p>生产者启动以来发送的数据总字节数
         *
         * @return 发送字节总数
         */
        long getTotalSentBytes();

        /**
         * 获取发送失败总数
         *
         * <p>生产者启动以来发送失败的消息总数
         *
         * @return 发送失败总数
         */
        long getTotalSendFailures();

        /**
         * 获取平均发送延迟（毫秒）
         *
         * <p>从调用发送方法到收到确认的平均时间
         *
         * @return 平均发送延迟，单位毫秒
         */
        double getAverageSendLatency();

        /**
         * 获取当前待发送消息数
         *
         * <p>当前在发送队列中等待发送的消息数量
         *
         * @return 待发送消息数
         */
        int getPendingMessages();
    }

    /**
     * 生产者异常类
     *
     * <p>封装生产者操作过程中可能出现的各种异常
     */
    class ProducerException extends Exception {
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
}
