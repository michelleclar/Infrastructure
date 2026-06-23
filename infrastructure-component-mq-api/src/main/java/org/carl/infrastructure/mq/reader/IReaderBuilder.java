package org.carl.infrastructure.mq.reader;

import org.carl.infrastructure.mq.common.ex.ReaderException;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Reader 构建器接口，提供流式 API 配置并创建 {@link IReader}。
 *
 * <p>用法示例：
 * <pre>{@code
 * IReader<byte[]> reader = mqClient.newReader()
 *     .startMessageId(ReaderStartPosition.Earliest)
 *     .readerName("my-reader")
 *     .create("persistent://er-tool/default/cdc-events");
 *
 * while (reader.hasMessageAvailable()) {
 *     Message<byte[]> msg = reader.readNext();
 *     // 处理消息 — Reader 无需 acknowledge
 * }
 * reader.close();
 * }</pre>
 *
 * @param <T> 消息类型
 */
public interface IReaderBuilder<T> {

    /**
     * 设置 topic（终止操作前也可通过 {@link #create(String)} 传入）。
     *
     * @param topic topic 全名
     * @return 构建器本身
     */
    IReaderBuilder<T> topic(String topic);

    /**
     * 按正则匹配多个 topic 并创建 Reader。
     *
     * <p>与 {@link #topic(String)} 互斥。pattern 须包含完整命名空间，例如
     * {@code persistent://public/default/cdc-events-.*}。{@code create()} 时会通过 PulsarAdmin
     * 一次性解析出当前匹配的全部 topic（分区 topic 折叠为基础名），再以 {@code topics(List)} 创建 Reader。
     * <b>解析为一次性</b>：创建后新增的 topic 不会被自动纳入。
     *
     * <p>匹配规则：将用户传入的正则原样编译，与每个 topic 全名（去掉 {@code persistent://} 等 scheme 前缀、
     * 去掉 {@code -partition-N} 后缀）做 {@link Pattern} 全匹配（{@code matcher(...).matches()}）。
     *
     * <p>需要 {@code ClientConfig.adminUrl()} 已配置；否则 {@code create()} 抛出
     * {@link ReaderException}。
     *
     * @param topicsPattern 选择 topic 的正则（不含分区后缀）
     * @return 构建器本身
     */
    IReaderBuilder<T> topicsPattern(Pattern topicsPattern);

    /**
     * 按正则匹配多个 topic 并创建 Reader（便捷方法，内部编译为 {@link Pattern}）。
     *
     * <p>语义同 {@link #topicsPattern(Pattern)}。
     *
     * @param topicsPattern 选择 topic 的正则（不含分区后缀）
     * @return 构建器本身
     */
    IReaderBuilder<T> topicsPattern(String topicsPattern);

    /**
     * 设置起始读取位置（Earliest / Latest）。
     *
     * @param position 起始位置枚举
     * @return 构建器本身
     */
    IReaderBuilder<T> startMessageId(ReaderStartPosition position);

    /**
     * 设置 Reader 名称（用于 topic stats 展示，可选）。
     *
     * @param readerName reader 名称
     * @return 构建器本身
     */
    IReaderBuilder<T> readerName(String readerName);

    /**
     * 设置 Reader 接收队列大小（可选，默认 1000）。
     *
     * @param receiverQueueSize 接收队列大小
     * @return 构建器本身
     */
    IReaderBuilder<T> receiverQueueSize(int receiverQueueSize);

    /**
     * 创建 Reader（topic 须已通过 {@link #topic(String)} 设置）。
     *
     * @return Reader 实例
     * @throws ReaderException 创建失败
     */
    IReader<T> create() throws ReaderException;

    /**
     * 设置 topic 并创建 Reader（便捷方法）。
     *
     * @param topic topic 全名
     * @return Reader 实例
     * @throws ReaderException 创建失败
     */
    IReader<T> create(String topic) throws ReaderException;

    /**
     * 异步创建 Reader（topic 须已通过 {@link #topic(String)} 设置）。
     *
     * @return Reader Future
     */
    CompletableFuture<IReader<T>> createAsync();
}
