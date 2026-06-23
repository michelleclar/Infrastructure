package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Reader;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.common.naming.TopicName;
import org.carl.infrastructure.mq.common.ex.ReaderException;
import org.carl.infrastructure.mq.reader.IReader;
import org.carl.infrastructure.mq.reader.IReaderBuilder;
import org.carl.infrastructure.mq.reader.ReaderStartPosition;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

class PulsarReaderBuilder<T> implements IReaderBuilder<T> {

    private final org.apache.pulsar.client.api.ReaderBuilder<T> readerBuilder;
    /** 用于 {@code topicsPattern} 解析；为 {@code null} 表示未配置 adminUrl，此时使用 pattern 会在创建阶段失败。 */
    private final PulsarAdmin pulsarAdmin;

    /** 用户设置的正则（原始字符串可经 {@link Pattern#pattern()} 取回，用于解析命名空间）。 */
    private Pattern topicsPattern;
    /** 标记是否已通过 {@link #topic(String)} 设置过单一 topic，用于与 topicsPattern 的互斥校验。 */
    private boolean topicSet;

    static PulsarReaderBuilder<byte[]> create(PulsarClient client, PulsarAdmin pulsarAdmin) {
        return new PulsarReaderBuilder<>(client, Schema.BYTES, pulsarAdmin);
    }

    static <T> PulsarReaderBuilder<T> create(PulsarClient client, Class<T> clazz, PulsarAdmin pulsarAdmin) {
        return new PulsarReaderBuilder<>(client, Schema.AVRO(clazz), pulsarAdmin);
    }

    private PulsarReaderBuilder(PulsarClient client, Schema<T> schema, PulsarAdmin pulsarAdmin) {
        this.readerBuilder = client.newReader(schema);
        this.pulsarAdmin = pulsarAdmin;
    }

    @Override
    public IReaderBuilder<T> topic(String topic) {
        this.topicSet = true;
        readerBuilder.topic(topic);
        return this;
    }

    @Override
    public IReaderBuilder<T> topicsPattern(Pattern topicsPattern) {
        this.topicsPattern = topicsPattern;
        return this;
    }

    @Override
    public IReaderBuilder<T> topicsPattern(String topicsPattern) {
        this.topicsPattern = Pattern.compile(topicsPattern);
        return this;
    }

    @Override
    public IReaderBuilder<T> startMessageId(ReaderStartPosition position) {
        MessageId msgId = switch (position) {
            case Earliest -> MessageId.earliest;
            case Latest -> MessageId.latest;
        };
        readerBuilder.startMessageId(msgId);
        return this;
    }

    @Override
    public IReaderBuilder<T> readerName(String readerName) {
        readerBuilder.readerName(readerName);
        return this;
    }

    @Override
    public IReaderBuilder<T> receiverQueueSize(int receiverQueueSize) {
        readerBuilder.receiverQueueSize(receiverQueueSize);
        return this;
    }

    @Override
    public IReader<T> create() throws ReaderException {
        try {
            if (topicsPattern != null) {
                rejectTopicIfSet();
                requireAdmin();
                List<String> matched = resolveTopics(topicsPattern);
                readerBuilder.topics(matched);
            }
            return new PulsarReader<>(readerBuilder.create());
        } catch (PulsarClientException e) {
            throw new ReaderException(e);
        }
    }

    @Override
    public IReader<T> create(String topic) throws ReaderException {
        if (topicsPattern != null) {
            throw new ReaderException("topicsPattern 与 create(topic) 互斥，不能同时使用");
        }
        readerBuilder.topic(topic);
        return create();
    }

    @Override
    public CompletableFuture<IReader<T>> createAsync() {
        if (topicsPattern == null) {
            return readerBuilder.createAsync().thenApply(PulsarReader::new);
        }
        try {
            rejectTopicIfSet();
            requireAdmin();
            Resolved resolved = resolve(topicsPattern);
            return pulsarAdmin
                    .topics()
                    .getListAsync(resolved.namespace)
                    .thenApply(all -> filterTopics(resolved.strippedPattern, all))
                    .thenCompose(
                            matched -> {
                                if (matched.isEmpty()) {
                                    return CompletableFuture.<Reader<T>>failedFuture(
                                            new ReaderException(
                                                    "no topics matched pattern: " + resolved.patternStr));
                                }
                                readerBuilder.topics(matched);
                                return readerBuilder.createAsync();
                            })
                    .<IReader<T>>thenApply(PulsarReader::new)
                    // 把异步链中的异常（admin 失败 / reader 创建失败）统一翻译为 ReaderException，
                    // 与同步 create() 的契约保持一致；已是 ReaderException 的原样透传。
                    .exceptionallyCompose(ex -> {
                        Throwable cause =
                                ex instanceof CompletionException && ex.getCause() != null
                                        ? ex.getCause()
                                        : ex;
                        return cause instanceof ReaderException re
                                ? CompletableFuture.failedFuture(re)
                                : CompletableFuture.failedFuture(
                                        new ReaderException(
                                                "按 topicsPattern 解析/创建 Reader 失败: "
                                                        + cause.getMessage(),
                                                cause));
                    });
        } catch (ReaderException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void requireAdmin() throws ReaderException {
        if (pulsarAdmin == null) {
            throw new ReaderException(
                    "使用 topicsPattern 需要在 ClientConfig 中配置 adminUrl (Pulsar Admin HTTP 地址)");
        }
    }

    private void rejectTopicIfSet() throws ReaderException {
        if (topicSet) {
            throw new ReaderException("topic(String) 与 topicsPattern 互斥，不能同时使用");
        }
    }

    /** 同步解析 pattern -> 匹配的 topic 列表。 */
    private List<String> resolveTopics(Pattern pattern) throws ReaderException {
        Resolved resolved = resolve(pattern);
        List<String> all;
        try {
            all = pulsarAdmin.topics().getList(resolved.namespace);
        } catch (PulsarAdminException e) {
            throw new ReaderException(
                    "查询命名空间 " + resolved.namespace + " 的 topic 列表失败: " + e.getMessage(), e);
        }
        List<String> matched = filterTopics(resolved.strippedPattern, all);
        if (matched.isEmpty()) {
            throw new ReaderException("没有匹配到 topic, pattern: " + resolved.patternStr);
        }
        return matched;
    }

    /**
     * 解析 pattern：用 {@link TopicName#get(String)} 提取命名空间（{@code tenant/namespace}），
     * 并把 <b>去掉 scheme 前缀</b>的用户正则原样编译。
     *
     * <p>注意：不能使用 Pulsar 的 {@code TopicName.getPattern}/{@code getPartitionPattern}——它们内部
     * 会用 {@code Pattern.quote} 把整个 topic 名转义为字面量，导致用户的正则（如 {@code .*}）失效，
     * 任何真实 topic 都匹配不到。Pulsar 自身的 Consumer 是在 broker 侧做正则匹配的，客户端这里需自行编译原始正则。
     */
    private Resolved resolve(Pattern pattern) throws ReaderException {
        String patternStr = pattern.pattern();
        try {
            String namespace = TopicName.get(patternStr).getNamespace();
            Pattern stripped = Pattern.compile(stripScheme(patternStr));
            return new Resolved(patternStr, namespace, stripped);
        } catch (RuntimeException e) {
            throw new ReaderException("无法解析 topicsPattern (命名空间或正则非法): " + patternStr, e);
        }
    }

    /**
     * 用「去掉 scheme 的用户正则」全匹配每个 topic（topic 侧同样去掉 scheme、并把分区名折叠为基础名），
     * 返回去重后的基础 topic 全名列表。
     *
     * <p>对分区 topic 折叠为基础名后去重，避免把同一逻辑 topic 的多个 {@code -partition-N} 都传给 Reader 造成重复订阅。
     *
     * <p>包级可见以便离线单测（无需 broker / admin）。
     */
    static List<String> filterTopics(Pattern strippedPattern, List<String> allTopics) {
        return allTopics.stream()
                .map(TopicName::get)
                .map(TopicName::getPartitionedTopicName) // 去掉 -partition-N，得到基础全名
                .distinct()
                .filter(base -> strippedPattern.matcher(stripScheme(base)).matches())
                .toList();
    }

    /** 去掉 topic 全名的 scheme 前缀（{@code persistent://} / {@code non-persistent://}）。 */
    private static String stripScheme(String name) {
        int i = name.indexOf("://");
        return i >= 0 ? name.substring(i + 3) : name;
    }

    /** 解析后的中间结果：原始 pattern、命名空间、去掉 scheme 的用户正则。 */
    private record Resolved(String patternStr, String namespace, Pattern strippedPattern) {}
}
