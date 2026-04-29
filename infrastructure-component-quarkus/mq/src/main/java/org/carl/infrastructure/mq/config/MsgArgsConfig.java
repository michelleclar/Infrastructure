package org.carl.infrastructure.mq.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import org.carl.infrastructure.mq.consumer.SubscriptionInitialPosition;
import org.carl.infrastructure.mq.consumer.SubscriptionType;
import org.carl.infrastructure.mq.producer.CompressionType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Pulsar MQ 配置参数
 *
 * <p>该接口用于将 Quarkus 配置系统中的 application.yaml / properties 绑定为 {@link MQConfig} 的具体实现。
 *
 * <p>领域语义、字段含义、业务注释： 👉 统一定义在 MQConfig 及其子接口中
 *
 * <p>本接口只关注：
 *
 * <ul>
 *   <li>配置前缀
 *   <li>字段映射名称
 *   <li>默认值
 * </ul>
 */
@ConfigMapping(prefix = "msg")
public interface MsgArgsConfig extends MQConfig {

    /** {@inheritDoc} */
    @Override
    @WithName("name")
    Optional<String> name();

    /** {@inheritDoc} */
    @WithName("client")
    ClientConfig client();

    /** {@inheritDoc} */
    @WithName("producer")
    ProducerConfig producer();

    /** {@inheritDoc} */
    @WithName("consumer")
    ConsumerConfig consumer();

    /** {@inheritDoc} */
    TransactionConfig transaction();

    /** {@inheritDoc} */
    MonitoringConfig monitoring();

    /** {@inheritDoc} */
    RetryConfig retry();

    /** {@inheritDoc} */
    interface ClientConfig extends MQConfig.ClientConfig {

        /** {@inheritDoc} */
        @WithDefault("pulsar://localhost:6650")
        @WithName("service-url")
        String serviceUrl();

        /** {@inheritDoc} */
        @WithName("auth-plugin-class-name")
        Optional<String> authPluginClassName();

        /** {@inheritDoc} */
        @WithName("auth-params")
        Optional<String> authParams();

        /** {@inheritDoc} */
        @WithName("auth-token")
        Optional<String> authToken();

        /** {@inheritDoc} */
        @WithDefault("30s")
        @WithName("operation-timeout")
        Duration operationTimeout();

        /** {@inheritDoc} */
        @WithDefault("10s")
        @WithName("connection-timeout")
        Duration connectionTimeout();

        /** {@inheritDoc} */
        @WithDefault("1")
        @WithName("connections-per-broker")
        int connectionsPerBroker();

        /** {@inheritDoc} */
        @WithDefault("true")
        @WithName("tcp-no-delay")
        boolean tcpNoDelay();

        /** {@inheritDoc} */
        @WithDefault("30s")
        @WithName("keep-alive-interval")
        Duration keepAliveInterval();

        /** {@inheritDoc} */
        @WithDefault("67108864") // 64MB
        @WithName("memory-limit")
        long memoryLimit();

        /** {@inheritDoc} */
        @WithDefault("50000")
        @WithName("max-lookup-requests")
        int maxLookupRequests();

        /** {@inheritDoc} */
        @WithDefault("20")
        @WithName("max-lookup-redirects")
        int maxLookupRedirects();

        /** {@inheritDoc} */
        @WithDefault("5000")
        @WithName("max-concurrent-lookup-requests")
        int maxConcurrentLookupRequests();

        /** {@inheritDoc} */
        TlsConfig tls();
    }

    /** {@inheritDoc} */
    interface TlsConfig extends MQConfig.TlsConfig {

        /** {@inheritDoc} */
        @WithDefault("false")
        boolean enabled();

        /** {@inheritDoc} */
        @WithName("trust-certs-file-path")
        Optional<String> trustCertsFilePath();

        /** {@inheritDoc} */
        @WithDefault("false")
        @WithName("allow-insecure-connection")
        boolean allowInsecureConnection();

        /** {@inheritDoc} */
        @WithDefault("false")
        @WithName("enable-hostname-verification")
        boolean enableHostnameVerification();
    }

    /** {@inheritDoc} */
    interface ProducerConfig extends MQConfig.ProducerConfig {

        /** {@inheritDoc} */
        @WithDefault("30s")
        @WithName("send-timeout")
        Duration sendTimeout();

        /** {@inheritDoc} */
        @WithDefault("true")
        @WithName("batching-enabled")
        boolean batchingEnabled();

        /** {@inheritDoc} */
        @WithDefault("1000")
        @WithName("batching-max-messages")
        int batchingMaxMessages();

        /** {@inheritDoc} */
        @WithDefault("1ms")
        @WithName("batching-max-publish-delay")
        Duration batchingMaxPublishDelay();

        /** {@inheritDoc} */
        @WithDefault("131072") // 128KB
        @WithName("batching-max-bytes")
        int batchingMaxBytes();

        /** {@inheritDoc} */
        @WithDefault("1000")
        @WithName("max-pending-messages")
        int maxPendingMessages();

        /** {@inheritDoc} */
        @WithDefault("BLOCK")
        @WithName("block-if-queue-full")
        String blockIfQueueFull();

        /** {@inheritDoc} */
        @WithDefault("none")
        @WithName("compression-type")
        CompressionType compressionType();

        /** {@inheritDoc} */
        @WithDefault("false")
        @WithName("chunking-enabled")
        boolean chunkingEnabled();

        /** {@inheritDoc} */
        @WithDefault("5242880") // 5MB
        @WithName("chunk-max-message-size")
        int chunkMaxMessageSize();
    }

    /** {@inheritDoc} */
    interface ConsumerConfig extends MQConfig.ConsumerConfig {
        /** {@inheritDoc} */
        @WithDefault("true")
        @WithName("auto-ack")
        Boolean autoAck();

        /** {@inheritDoc} */
        @WithDefault("30s")
        @WithName("ack-timeout")
        Duration ackTimeout();

        /** {@inheritDoc} */
        @WithDefault("1s")
        @WithName("ack-timeout-tick-time")
        Duration ackTimeoutTickTime();

        /** {@inheritDoc} */
        @WithDefault("1m")
        @WithName("negative-ack-redelivery-delay")
        Duration negativeAckRedeliveryDelay();

        /** {@inheritDoc} */
        @WithDefault("1000")
        @WithName("receiver-queue-size")
        int receiverQueueSize();

        /** {@inheritDoc} */
        @WithDefault("3")
        @WithName("max-redeliver-count")
        int maxRedeliverCount();

        /** {@inheritDoc} */
        @WithDefault("-dlq")
        @WithName("dead-letter-topic-suffix")
        String deadLetterTopicSuffix();

        /** {@inheritDoc} */
        @WithDefault("-retry")
        @WithName("retry-topic-suffix")
        String retryTopicSuffix();

        /** {@inheritDoc} */
        @WithDefault("false")
        @WithName("batch-receive-enabled")
        boolean batchReceiveEnabled();

        /** {@inheritDoc} */
        @WithDefault("100")
        @WithName("batch-receive-max-messages")
        int batchReceiveMaxMessages();

        /** {@inheritDoc} */
        @WithDefault("100ms")
        @WithName("batch-receive-timeout")
        Duration batchReceiveTimeout();

        /** {@inheritDoc} */
        @WithDefault("LATEST")
        @WithName("subscription-initial-position")
        SubscriptionInitialPosition subscriptionInitialPosition();

        /** {@inheritDoc} */
        @WithDefault("0")
        int priority();

        /** {@inheritDoc} */
        @WithDefault("false")
        @WithName("read-compacted")
        boolean readCompacted();

        /** {@inheritDoc} */
        @WithDefault("Exclusive")
        @WithName("subscription-type")
        SubscriptionType subscriptionType();
    }

    /** {@inheritDoc} */
    interface TransactionConfig extends MQConfig.TransactionConfig {

        /** {@inheritDoc} */
        @WithDefault("false")
        boolean enabled();

        /** {@inheritDoc} */
        @WithDefault("persistent://pulsar/system/transaction_coordinator_assign")
        @WithName("coordinator-topic")
        String coordinatorTopic();

        /** {@inheritDoc} */
        @WithDefault("60s")
        @WithName("timeout")
        Duration timeout();

        /** {@inheritDoc} */
        @WithDefault("262144") // 256KB
        @WithName("buffer-snapshot-segment-size")
        int bufferSnapshotSegmentSize();

        /** {@inheritDoc} */
        @WithDefault("5s")
        @WithName("buffer-snapshot-min-time-in-millis")
        Duration bufferSnapshotMinTime();

        /** {@inheritDoc} */
        @WithDefault("1000")
        @WithName("buffer-snapshot-max-transaction-count")
        int bufferSnapshotMaxTransactionCount();
    }

    /** {@inheritDoc} */
    interface MonitoringConfig extends MQConfig.MonitoringConfig {

        /** {@inheritDoc} */
        @WithDefault("true")
        @WithName("metrics-enabled")
        boolean metricsEnabled();

        /** {@inheritDoc} */
        @WithDefault("60s")
        @WithName("stats-interval")
        Duration statsInterval();

        /** {@inheritDoc} */
        @WithDefault("true")
        @WithName("topic-level-metrics-enabled")
        boolean topicLevelMetricsEnabled();

        /** {@inheritDoc} */
        @WithDefault("true")
        @WithName("consumer-level-metrics-enabled")
        boolean consumerLevelMetricsEnabled();

        /** {@inheritDoc} */
        @WithDefault("true")
        @WithName("producer-level-metrics-enabled")
        boolean producerLevelMetricsEnabled();
    }

    /** {@inheritDoc} */
    interface RetryConfig extends MQConfig.RetryConfig {

        /** {@inheritDoc} */
        @WithDefault("3")
        @WithName("max-attempts")
        int maxAttempts();

        /** {@inheritDoc} */
        @WithDefault("1s")
        @WithName("initial-delay")
        Duration initialDelay();

        /** {@inheritDoc} */
        @WithDefault("30s")
        @WithName("max-delay")
        Duration maxDelay();

        /** {@inheritDoc} */
        @WithDefault("2.0")
        @WithName("multiplier")
        double multiplier();

        /** {@inheritDoc} */
        @WithName("retryable-exceptions")
        Optional<List<String>> retryableExceptions();
    }
}
