package org.carl.infrastructure.mq.config;

import org.carl.infrastructure.mq.consumer.SubscriptionInitialPosition;
import org.carl.infrastructure.mq.consumer.SubscriptionType;
import org.carl.infrastructure.mq.producer.CompressionType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** MQ config */
public interface MQConfig {
    Optional<String> name();

    /** 客户端配置 */
    ClientConfig client();

    /** 生产者默认配置 */
    ProducerConfig producer();

    /** 消费者默认配置 */
    ConsumerConfig consumer();

    /** 事务配置 */
    TransactionConfig transaction();

    /** 监控配置 */
    MonitoringConfig monitoring();

    /** 重试策略配置 */
    RetryConfig retry();

    /** 客户端配置接口 */
    interface ClientConfig {

        /** Pulsar 服务地址 */
        String serviceUrl();

        /** 认证插件类名 */
        Optional<String> authPluginClassName();

        /** 认证参数 */
        Optional<String> authParams();

        /** 认证 Token */
        Optional<String> authToken();

        /** 操作超时时间 */
        Duration operationTimeout();

        /** 连接超时时间 */
        Duration connectionTimeout();

        /** 最大连接数 */
        int connectionsPerBroker();

        /** 是否启用 TCP 无延迟 */
        boolean tcpNoDelay();

        /** 保活间隔 */
        Duration keepAliveInterval();

        /** 内存限制（字节） */
        long memoryLimit();

        /** 最大查找请求数 */
        int maxLookupRequests();

        /** 最大查找重定向次数 */
        int maxLookupRedirects();

        /** 最大并发查找请求数 */
        int maxConcurrentLookupRequests();

        /** TLS 配置 */
        TlsConfig tls();
    }

    /** TLS 配置接口 */
    interface TlsConfig {

        /** 是否启用 TLS */
        boolean enabled();

        /** 信任证书文件路径 */
        Optional<String> trustCertsFilePath();

        /** 是否允许不安全连接 */
        boolean allowInsecureConnection();

        /** 是否启用主机名验证 */
        boolean enableHostnameVerification();
    }

    /** 生产者配置接口 */
    interface ProducerConfig {

        /** 发送超时时间 */
        Duration sendTimeout();

        /** 是否启用批量发送 */
        boolean batchingEnabled();

        /** 批量最大消息数 */
        int batchingMaxMessages();

        /** 批量最大发布延迟 */
        Duration batchingMaxPublishDelay();

        /** 批量最大字节数 */
        int batchingMaxBytes();

        /** 最大待发送消息数 */
        int maxPendingMessages();

        /** 待发送消息队列满时的处理策略 */
        String blockIfQueueFull();

        /** 压缩类型 */
        CompressionType compressionType();

        /** 是否启用分块 */
        boolean chunkingEnabled();

        /** 最大消息大小 */
        int chunkMaxMessageSize();
    }

    /** 消费者配置接口 */
    interface ConsumerConfig {

        default Boolean autoAck() {
            return false;
        }

        /** 确认超时时间 */
        Duration ackTimeout();

        /** 确认超时重新投递延迟 */
        Duration ackTimeoutTickTime();

        /** 否定确认重新投递延迟 */
        Duration negativeAckRedeliveryDelay();

        /** 接收队列大小 */
        int receiverQueueSize();

        /** 最大重新投递次数 */
        int maxRedeliverCount();

        /** 死信队列主题后缀 */
        String deadLetterTopicSuffix();

        /** 重试队列主题后缀 */
        String retryTopicSuffix();

        /** 是否启用批量接收 */
        boolean batchReceiveEnabled();

        /** 批量接收最大消息数 */
        int batchReceiveMaxMessages();

        /** 批量接收超时时间 */
        Duration batchReceiveTimeout();

        /** 订阅初始位置 */
        SubscriptionInitialPosition subscriptionInitialPosition();

        /** 优先级 */
        int priority();

        /** 是否只读复制 */
        boolean readCompacted();

        /** 是否只读复制 */
        SubscriptionType subscriptionType();
    }

    /** 事务配置接口 */
    interface TransactionConfig {

        /** 是否启用事务 */
        boolean enabled();

        /** 事务协调器主题 */
        String coordinatorTopic();

        /** 默认事务超时时间 */
        Duration timeout();

        /** 事务缓冲区快照段大小 */
        int bufferSnapshotSegmentSize();

        /** 事务缓冲区快照最小时间间隔 */
        Duration bufferSnapshotMinTime();

        /** 事务缓冲区快照最大事务数 */
        int bufferSnapshotMaxTransactionCount();
    }

    /** 监控配置接口 */
    interface MonitoringConfig {

        /** 是否启用指标收集 */
        boolean metricsEnabled();

        /** 统计间隔 */
        Duration statsInterval();

        /** 是否启用主题级别统计 */
        boolean topicLevelMetricsEnabled();

        /** 是否启用消费者级别统计 */
        boolean consumerLevelMetricsEnabled();

        /** 是否启用生产者级别统计 */
        boolean producerLevelMetricsEnabled();
    }

    /** 重试策略配置接口 */
    interface RetryConfig {

        /** 默认最大重试次数 */
        int maxAttempts();

        /** 初始重试延迟 */
        Duration initialDelay();

        /** 最大重试延迟 */
        Duration maxDelay();

        /** 重试延迟倍数 */
        double multiplier();

        /** 可重试的异常类型 */
        Optional<List<String>> retryableExceptions();
    }
}
