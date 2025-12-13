package org.carl.infrastructure.mq.consumer;

import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.model.Message;

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
    Message<T> receive() throws ConsumerException;

    /**
     * 同步接收消息，带超时
     *
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 接收到的消息，超时返回 null
     * @throws ConsumerException 接收异常
     */
    Message<T> receive(int timeout, TimeUnit unit) throws ConsumerException;

    /**
     * 异步接收消息
     *
     * @return 消息的 Future
     */
    CompletableFuture<Message<T>> receiveAsync() throws ConsumerException;

    // ===== 批量消费功能 =====

    // ===== 消息确认功能 =====

    /**
     * 确认消息
     *
     * @param message 要确认的消息
     * @throws ConsumerException 确认异常
     */
    void acknowledge(Message<T> message) throws ConsumerException;

    /**
     * 异步确认消息
     *
     * @param message 要确认的消息
     * @return 确认完成的 Future
     */
    CompletableFuture<Void> acknowledgeAsync(Message<T> message);

    /**
     * 累积确认消息（确认到指定消息为止的所有消息）
     *
     * @param message 累积确认的最后一条消息
     * @throws ConsumerException 确认异常
     */
    void acknowledgeCumulative(Message<T> message) throws ConsumerException;

    /**
     * 异步累积确认消息
     *
     * @param message 累积确认的最后一条消息
     * @return 确认完成的 Future
     */
    CompletableFuture<Void> acknowledgeCumulativeAsync(Message<T> message);

    /**
     * 否定确认消息（触发重新投递）
     *
     * @param message 要否定确认的消息
     */
    void negativeAcknowledge(Message<T> message);

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

    // ===== 枚举和统计类 =====

}
