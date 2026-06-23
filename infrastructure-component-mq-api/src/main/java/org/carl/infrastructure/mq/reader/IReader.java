package org.carl.infrastructure.mq.reader;

import org.carl.infrastructure.mq.common.ex.ReaderException;
import org.carl.infrastructure.mq.model.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 消息 Reader 接口。
 *
 * <p>Reader 不维护 subscription，不需要 acknowledge，可从任意位置读取消息而不影响现有消费者的进度。
 * 适用于消息回放、调试和审计场景。</p>
 *
 * @param <T> 消息类型
 */
public interface IReader<T> extends AutoCloseable {

    /**
     * 同步读取下一条消息（阻塞，直到有消息可读）。
     *
     * @return 下一条消息
     * @throws ReaderException 读取异常
     */
    Message<T> readNext() throws ReaderException;

    /**
     * 同步读取下一条消息，带超时。
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 下一条消息，超时返回 null
     * @throws ReaderException 读取异常
     */
    Message<T> readNext(int timeout, TimeUnit unit) throws ReaderException;

    /**
     * 异步读取下一条消息。
     *
     * @return 消息的 Future
     */
    CompletableFuture<Message<T>> readNextAsync();

    /**
     * 检查当前位置是否有消息可读。
     *
     * @return true 表示有消息
     * @throws ReaderException 检查异常
     */
    boolean hasMessageAvailable() throws ReaderException;

    /**
     * Seek 到指定时间戳（毫秒），Reader 将从该时间点之后的第一条消息开始读取。
     *
     * @param timestamp 时间戳（epoch ms）
     * @throws ReaderException seek 异常
     */
    void seek(long timestamp) throws ReaderException;

    /**
     * 检查 Reader 是否已连接到 broker。
     *
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 关闭 Reader，释放底层资源。
     *
     * @throws ReaderException 关闭异常
     */
    @Override
    void close() throws ReaderException;

    /**
     * 异步关闭 Reader。
     *
     * @return 关闭完成的 Future
     */
    CompletableFuture<Void> closeAsync();
}
