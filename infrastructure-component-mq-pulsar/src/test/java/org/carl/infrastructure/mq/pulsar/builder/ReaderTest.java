package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.PulsarClient;
import org.carl.infrastructure.mq.common.ex.ReaderException;
import org.carl.infrastructure.mq.reader.IReader;
import org.carl.infrastructure.mq.reader.IReaderBuilder;
import org.carl.infrastructure.mq.reader.ReaderStartPosition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reader / topicsPattern 测试。
 *
 * <p>{@code filterTopics} 用例直接验证核心匹配算法（离线、无需 broker）；guard 用例同样可离线运行；
 * happy-path 用例需要真实 Pulsar（serviceUrl + adminUrl），标记 {@link Disabled}，参见 {@code ConsumerTest}。
 */
class ReaderTest {

    /** 创建一个未连接的 PulsarClient（build() 惰性连接，不会阻塞）。 */
    private static PulsarClient unconnectedClient() throws Exception {
        return PulsarClient.builder()
                .serviceUrl("pulsar://localhost:1")
                .operationTimeout(1, TimeUnit.SECONDS)
                .build();
    }

    /** 核心：用户正则（去掉 scheme）必须按正则匹配多个 topic，而非字面匹配。 */
    @Test
    void filterTopicsMatchesByRegex() {
        Pattern stripped = Pattern.compile("public/default/cdc-events-.*");
        List<String> all =
                List.of(
                        "persistent://public/default/cdc-events-1",
                        "persistent://public/default/cdc-events-2",
                        "persistent://public/default/other");
        List<String> matched = PulsarReaderBuilder.filterTopics(stripped, all);
        assertEquals(2, matched.size());
        assertTrue(matched.contains("persistent://public/default/cdc-events-1"));
        assertTrue(matched.contains("persistent://public/default/cdc-events-2"));
    }

    /** 分区 topic 折叠为基础名并去重，避免重复订阅。 */
    @Test
    void filterTopicsCollapsesPartitionsToBase() {
        Pattern stripped = Pattern.compile("public/default/cdc-events-1");
        List<String> all =
                List.of(
                        "persistent://public/default/cdc-events-1-partition-0",
                        "persistent://public/default/cdc-events-1-partition-1");
        List<String> matched = PulsarReaderBuilder.filterTopics(stripped, all);
        assertEquals(List.of("persistent://public/default/cdc-events-1"), matched);
    }

    /** 使用 topicsPattern 但未配置 adminUrl（admin 为 null）→ create() 抛 ReaderException。 */
    @Test
    void topicsPatternRequiresAdminUrl() throws Exception {
        try (PulsarClient client = unconnectedClient()) {
            IReaderBuilder<byte[]> builder =
                    PulsarReaderBuilder.create(client, null)
                            .topicsPattern("persistent://public/default/cdc-.*");
            ReaderException ex = assertThrows(ReaderException.class, builder::create);
            assertTrue(ex.getMessage().contains("adminUrl"), "message: " + ex.getMessage());
        }
    }

    /** createAsync() 在无 admin 时不得同步抛出，而应返回已异常完成的 future（cause 为 ReaderException）。 */
    @Test
    void createAsyncWithoutAdminReturnsFailedFuture() throws Exception {
        try (PulsarClient client = unconnectedClient()) {
            CompletableFuture<IReader<byte[]>> future =
                    PulsarReaderBuilder.create(client, null)
                            .topicsPattern("persistent://public/default/cdc-.*")
                            .createAsync();
            Throwable cause = future.handle((v, t) -> t).get(2, TimeUnit.SECONDS);
            assertNotNull(cause, "future should complete exceptionally");
            assertTrue(cause instanceof ReaderException, "cause: " + cause);
            assertTrue(cause.getMessage().contains("adminUrl"));
        }
    }

    /** topic(String) 与 topicsPattern 互斥（create 路径）。 */
    @Test
    void topicAndTopicsPatternAreMutuallyExclusive() throws Exception {
        try (PulsarClient client = unconnectedClient()) {
            IReaderBuilder<byte[]> builder =
                    PulsarReaderBuilder.create(client, null)
                            .topic("persistent://public/default/cdc-1")
                            .topicsPattern("persistent://public/default/cdc-.*");
            assertThrows(ReaderException.class, builder::create);
        }
    }

    /** topicsPattern 与 create(topic) 互斥。 */
    @Test
    void topicsPatternAndCreateTopicAreMutuallyExclusive() throws Exception {
        try (PulsarClient client = unconnectedClient()) {
            IReaderBuilder<byte[]> builder =
                    PulsarReaderBuilder.create(client, null)
                            .topicsPattern(Pattern.compile("persistent://public/default/cdc-.*"));
            assertThrows(
                    ReaderException.class,
                    () -> builder.create("persistent://public/default/cdc-1"));
        }
    }

    /**
     * Happy-path：对真实 Pulsar 用 topicsPattern 读取匹配的多个 topic。
     *
     * <p>需要可用的 serviceUrl + adminUrl，并预先在 {@code public/default} 下创建形如
     * {@code infra-reader-demo-1} / {@code infra-reader-demo-2} 的 topic 并写入消息。
     */
    @Disabled("requires live Pulsar broker + adminUrl")
    @Test
    void readTopicsByPattern() throws Exception {
        String serviceUrl = "pulsar://180.184.66.147:6650";
        String adminUrl = "http://180.184.66.147:8080";
        try (PulsarClient client =
                PulsarClient.builder().serviceUrl(serviceUrl).operationTimeout(10, TimeUnit.SECONDS).build()) {
            try (org.apache.pulsar.client.admin.PulsarAdmin admin =
                    org.apache.pulsar.client.admin.PulsarAdmin.builder().serviceHttpUrl(adminUrl).build()) {
                IReaderBuilder<byte[]> builder =
                        PulsarReaderBuilder.create(client, admin)
                                .topicsPattern("persistent://public/default/infra-reader-demo-.*")
                                .startMessageId(ReaderStartPosition.Earliest);
                try (IReader<byte[]> reader = builder.create()) {
                    while (reader.hasMessageAvailable()) {
                        System.out.println("---- " + new String(reader.readNext().getValue()));
                    }
                }
            }
        }
    }
}
