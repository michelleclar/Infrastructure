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

/** Pulsar 配置参数 使用 Quarkus ConfigMapping 进行配置绑定 */
@ConfigMapping(prefix = "msg")
public interface MsgArgsConfig {

    /** 客户端配置 */
    @WithName("client")
    ClientConfig client();

    /** 生产者默认配置 */
    @WithName("producer")
    ProducerConfig producer();

    /** 消费者默认配置 */
    @WithName("consumer")
    ConsumerConfig consumer();

    /** 事务配置 */
    TransactionConfig transaction();

    /** 客户端配置接口 */
    interface ClientConfig {

        /** Pulsar 服务地址 */
        @WithDefault("pulsar://localhost:6650")
        @WithName("service-url")
        String serviceUrl();

        /** 认证插件类名 */
        @WithName("auth-plugin-class-name")
        Optional<String> authPluginClassName();

        /** 认证参数 */
        @WithName("auth-params")
        Optional<String> authParams();

        /** 认证 Token */
        @WithName("auth-token")
        Optional<String> authToken();

        /** 操作超时时间 */
        @WithDefault("30s")
        @WithName("operation-timeout")
        Duration operationTimeout();

        /** 连接超时时间 */
        @WithDefault("10s")
        @WithName("connection-timeout")
        Duration connectionTimeout();

        /** 最大连接数 */
        @WithDefault("1")
        @WithName("connections-per-broker")
        int connectionsPerBroker();

        /** 是否启用 TCP 无延迟 */
        @WithDefault("true")
        @WithName("tcp-no-delay")
        boolean tcpNoDelay();

        /** 保活间隔 */
        @WithDefault("30s")
        @WithName("keep-alive-interval")
        Duration keepAliveInterval();

        /** 内存限制（字节） */
        @WithDefault("67108864") // 64MB
        @WithName("memory-limit")
        long memoryLimit();

        /** 最大查找请求数 */
        @WithDefault("50000")
        @WithName("max-lookup-requests")
        int maxLookupRequests();

        /** 最大查找重定向次数 */
        @WithDefault("20")
        @WithName("max-lookup-redirects")
        int maxLookupRedirects();

        /** 最大并发查找请求数 */
        @WithDefault("5000")
        @WithName("max-concurrent-lookup-requests")
        int maxConcurrentLookupRequests();

        /** TLS 配置 */
        TlsConfig tls();
    }

    /** TLS 配置接口 */
    interface TlsConfig {

        /** 是否启用 TLS */
        @WithDefault("false")
        boolean enabled();

        /** 信任证书文件路径 */
        @WithName("trust-certs-file-path")
        Optional<String> trustCertsFilePath();

        /** 是否允许不安全连接 */
        @WithDefault("false")
        @WithName("allow-insecure-connection")
        boolean allowInsecureConnection();

        /** 是否启用主机名验证 */
        @WithDefault("false")
        @WithName("enable-hostname-verification")
        boolean enableHostnameVerification();
    }

    /** 生产者配置接口 */
    interface ProducerConfig {

        /** 发送超时时间 */
        @WithDefault("30s")
        @WithName("send-timeout")
        Duration sendTimeout();

        /** 是否启用批量发送 */
        @WithDefault("true")
        @WithName("batching-enabled")
        boolean batchingEnabled();

        /** 批量最大消息数 */
        @WithDefault("1000")
        @WithName("batching-max-messages")
        int batchingMaxMessages();

        /** 批量最大发布延迟 */
        @WithDefault("1ms")
        @WithName("batching-max-publish-delay")
        Duration batchingMaxPublishDelay();

        /** 批量最大字节数 */
        @WithDefault("131072") // 128KB
        @WithName("batching-max-bytes")
        int batchingMaxBytes();

        /** 最大待发送消息数 */
        @WithDefault("1000")
        @WithName("max-pending-messages")
        int maxPendingMessages();

        /** 待发送消息队列满时的处理策略 */
        @WithDefault("BLOCK")
        @WithName("block-if-queue-full")
        String blockIfQueueFull();

        /** 压缩类型 */
        @WithDefault("none")
        @WithName("compression-type")
        CompressionType compressionType();

        /** 是否启用分块 */
        @WithDefault("false")
        @WithName("chunking-enabled")
        boolean chunkingEnabled();

        /** 最大消息大小 */
        @WithDefault("5242880") // 5MB
        @WithName("chunk-max-message-size")
        int chunkMaxMessageSize();
    }

    /** 消费者配置接口 */
    interface ConsumerConfig {

        /** 确认超时时间 */
        @WithDefault("30s")
        @WithName("ack-timeout")
        Duration ackTimeout();

        /** 确认超时重新投递延迟 */
        @WithDefault("1s")
        @WithName("ack-timeout-tick-time")
        Duration ackTimeoutTickTime();

        /** 否定确认重新投递延迟 */
        @WithDefault("1m")
        @WithName("negative-ack-redelivery-delay")
        Duration negativeAckRedeliveryDelay();

        /** 接收队列大小 */
        @WithDefault("1000")
        @WithName("receiver-queue-size")
        int receiverQueueSize();

        /** 最大重新投递次数 */
        @WithDefault("3")
        @WithName("max-redeliver-count")
        int maxRedeliverCount();

        /** 死信队列主题后缀 */
        @WithDefault("-dlq")
        @WithName("dead-letter-topic-suffix")
        String deadLetterTopicSuffix();

        /** 重试队列主题后缀 */
        @WithDefault("-retry")
        @WithName("retry-topic-suffix")
        String retryTopicSuffix();

        /** 是否启用批量接收 */
        @WithDefault("false")
        @WithName("batch-receive-enabled")
        boolean batchReceiveEnabled();

        /** 批量接收最大消息数 */
        @WithDefault("100")
        @WithName("batch-receive-max-messages")
        int batchReceiveMaxMessages();

        /** 批量接收超时时间 */
        @WithDefault("100ms")
        @WithName("batch-receive-timeout")
        Duration batchReceiveTimeout();

        /** 订阅初始位置 */
        @WithDefault("LATEST")
        @WithName("subscription-initial-position")
        SubscriptionInitialPosition subscriptionInitialPosition();
        /** 优先级 */
        @WithDefault("0")
        int priority();

        /** 是否只读复制 */
        @WithDefault("false")
        @WithName("read-compacted")
        boolean readCompacted();

        /** 是否只读复制 */
        @WithDefault("Exclusive")
        @WithName("subscription-type")
        SubscriptionType subscriptionType();
    }

    /** 事务配置接口 */
    interface TransactionConfig {

        /** 是否启用事务 */
        @WithDefault("false")
        boolean enabled();

        /** 事务协调器主题 */
        @WithDefault("persistent://pulsar/system/transaction_coordinator_assign")
        @WithName("coordinator-topic")
        String coordinatorTopic();

        /** 默认事务超时时间 */
        @WithDefault("60s")
        @WithName("timeout")
        Duration timeout();

        /** 事务缓冲区快照段大小 */
        @WithDefault("262144") // 256KB
        @WithName("buffer-snapshot-segment-size")
        int bufferSnapshotSegmentSize();

        /** 事务缓冲区快照最小时间间隔 */
        @WithDefault("5s")
        @WithName("buffer-snapshot-min-time-in-millis")
        Duration bufferSnapshotMinTime();

        /** 事务缓冲区快照最大事务数 */
        @WithDefault("1000")
        @WithName("buffer-snapshot-max-transaction-count")
        int bufferSnapshotMaxTransactionCount();
    }

    /** 监控配置 */
    MonitoringConfig monitoring();

    /** 监控配置接口 */
    interface MonitoringConfig {

        /** 是否启用指标收集 */
        @WithDefault("true")
        @WithName("metrics-enabled")
        boolean metricsEnabled();

        /** 统计间隔 */
        @WithDefault("60s")
        @WithName("stats-interval")
        Duration statsInterval();

        /** 是否启用主题级别统计 */
        @WithDefault("true")
        @WithName("topic-level-metrics-enabled")
        boolean topicLevelMetricsEnabled();

        /** 是否启用消费者级别统计 */
        @WithDefault("true")
        @WithName("consumer-level-metrics-enabled")
        boolean consumerLevelMetricsEnabled();

        /** 是否启用生产者级别统计 */
        @WithDefault("true")
        @WithName("producer-level-metrics-enabled")
        boolean producerLevelMetricsEnabled();
    }

    /** 重试策略配置 */
    RetryConfig retry();

    /** 重试策略配置接口 */
    interface RetryConfig {

        /** 默认最大重试次数 */
        @WithDefault("3")
        @WithName("max-attempts")
        int maxAttempts();

        /** 初始重试延迟 */
        @WithDefault("1s")
        @WithName("initial-delay")
        Duration initialDelay();

        /** 最大重试延迟 */
        @WithDefault("30s")
        @WithName("max-delay")
        Duration maxDelay();

        /** 重试延迟倍数 */
        @WithDefault("2.0")
        @WithName("multiplier")
        double multiplier();

        /** 可重试的异常类型 */
        @WithName("retryable-exceptions")
        Optional<List<String>> retryableExceptions();
    }
}
