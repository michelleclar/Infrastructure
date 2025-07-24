package org.carl.infrastructure.pulsar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** Pulsar 配置验证器 验证配置的合理性 */
public class PulsarConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(PulsarConfigValidator.class);
    private final MsgArgsConfig config;

    public PulsarConfigValidator(MsgArgsConfig config) {
        this.config = config;
    }

    public void validate() {
        logger.info("Validating Pulsar configuration...");

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        validateClientConfig(warnings, errors);
        validateProducerConfig(warnings, errors);
        validateConsumerConfig(warnings, errors);
        validateTransactionConfig(warnings, errors);
        validateMonitoringConfig(warnings, errors);
        validateRetryConfig(warnings, errors);

        // 输出警告
        if (!warnings.isEmpty()) {
            logger.warn("Pulsar configuration warnings:");
            warnings.forEach(warning -> logger.warn("  - {}", warning));
        }

        // 输出错误并抛出异常
        if (!errors.isEmpty()) {
            logger.error("Pulsar configuration errors:");
            errors.forEach(error -> logger.error("  - {}", error));
            throw new IllegalArgumentException(
                    "Invalid Pulsar configuration. Please check the errors above.");
        }

        logger.info("Pulsar configuration validation completed successfully");
        logConfigurationSummary();
    }

    private void validateClientConfig(List<String> warnings, List<String> errors) {
        MsgArgsConfig.ClientConfig clientConfig = config.client();

        // 验证服务地址
        if (clientConfig.serviceUrl() == null || clientConfig.serviceUrl().trim().isEmpty()) {
            errors.add("Client service URL cannot be empty");
        } else if (!clientConfig.serviceUrl().startsWith("pulsar://")
                && !clientConfig.serviceUrl().startsWith("pulsar+ssl://")) {
            warnings.add("Service URL should start with 'pulsar://' or 'pulsar+ssl://'");
        }

        // 验证超时配置
        if (clientConfig.operationTimeout().toSeconds() < 1) {
            warnings.add("Operation timeout is very short: " + clientConfig.operationTimeout());
        }

        if (clientConfig.connectionTimeout().toSeconds() < 1) {
            warnings.add("Connection timeout is very short: " + clientConfig.connectionTimeout());
        }

        // 验证连接数配置
        if (clientConfig.connectionsPerBroker() < 1) {
            errors.add("Connections per broker must be at least 1");
        } else if (clientConfig.connectionsPerBroker() > 10) {
            warnings.add("High connections per broker: " + clientConfig.connectionsPerBroker());
        }

        // 验证内存限制
        if (clientConfig.memoryLimit() < 1024 * 1024) { // 1MB
            warnings.add("Memory limit is very low: " + clientConfig.memoryLimit() + " bytes");
        }

        // 验证 TLS 配置
        if (clientConfig.tls().enabled()) {
            if (clientConfig.serviceUrl().startsWith("pulsar://")) {
                warnings.add("TLS is enabled but service URL uses non-SSL scheme");
            }

            if (clientConfig.tls().allowInsecureConnection()
                    && clientConfig.tls().enableHostnameVerification()) {
                warnings.add(
                        "Hostname verification is enabled but insecure connections are allowed");
            }
        }

        // 验证认证配置
        if (clientConfig.authToken().isPresent()
                && clientConfig.authPluginClassName().isPresent()) {
            warnings.add("Both auth token and auth plugin are configured, token will be used");
        }
    }

    private void validateProducerConfig(List<String> warnings, List<String> errors) {
        MsgArgsConfig.ProducerConfig producerConfig = config.producer();

        // 验证发送超时
        if (producerConfig.sendTimeout().toSeconds() < 1) {
            warnings.add("Producer send timeout is very short: " + producerConfig.sendTimeout());
        }

        // 验证批量配置
        if (producerConfig.batchingEnabled()) {
            if (producerConfig.batchingMaxMessages() < 1) {
                errors.add("Batching max messages must be at least 1");
            }

            if (producerConfig.batchingMaxBytes() < 1024) {
                warnings.add(
                        "Batching max bytes is very small: " + producerConfig.batchingMaxBytes());
            }

            if (producerConfig.batchingMaxPublishDelay().toMillis() < 1) {
                warnings.add(
                        "Batching max publish delay is very short: "
                                + producerConfig.batchingMaxPublishDelay());
            }
        }

        // 验证队列配置
        if (producerConfig.maxPendingMessages() < 1) {
            errors.add("Max pending messages must be at least 1");
        } else if (producerConfig.maxPendingMessages() > 100000) {
            warnings.add("Very high max pending messages: " + producerConfig.maxPendingMessages());
        }

        // 验证消息大小
        if (producerConfig.chunkMaxMessageSize() < 1024) {
            warnings.add("Max message size is very small: " + producerConfig.chunkMaxMessageSize());
        } else if (producerConfig.chunkMaxMessageSize() > 100 * 1024 * 1024) { // 100MB
            warnings.add("Very large max message size: " + producerConfig.chunkMaxMessageSize());
        }

        //        // 验证压缩类型
        //        try {
        //
        // org.apache.pulsar.client.api.CompressionType.valueOf(producerConfig.compressionType());
        //        } catch (IllegalArgumentException e) {
        //            errors.add("Invalid compression type: " + producerConfig.compressionType());
        //        }
    }

    private void validateConsumerConfig(List<String> warnings, List<String> errors) {
        MsgArgsConfig.ConsumerConfig consumerConfig = config.consumer();

        // 验证确认超时
        if (consumerConfig.ackTimeout().toSeconds() < 1) {
            warnings.add("Consumer ack timeout is very short: " + consumerConfig.ackTimeout());
        }

        // 验证接收队列大小
        if (consumerConfig.receiverQueueSize() < 1) {
            errors.add("Receiver queue size must be at least 1");
        } else if (consumerConfig.receiverQueueSize() > 10000) {
            warnings.add("Very large receiver queue size: " + consumerConfig.receiverQueueSize());
        }

        // 验证重试配置
        if (consumerConfig.maxRedeliverCount() < 0) {
            errors.add("Max redeliver count cannot be negative");
        } else if (consumerConfig.maxRedeliverCount() > 100) {
            warnings.add("Very high max redeliver count: " + consumerConfig.maxRedeliverCount());
        }

        // 验证批量接收配置
        if (consumerConfig.batchReceiveEnabled()) {
            if (consumerConfig.batchReceiveMaxMessages() < 1) {
                errors.add("Batch receive max messages must be at least 1");
            }

            if (consumerConfig.batchReceiveTimeout().toMillis() < 1) {
                warnings.add(
                        "Batch receive timeout is very short: "
                                + consumerConfig.batchReceiveTimeout());
            }
        }

        //        // 验证订阅初始位置
        //        try {
        //            org.apache.pulsar.client.api.SubscriptionInitialPosition.valueOf(
        //                    consumerConfig.subscriptionInitialPosition());
        //        } catch (IllegalArgumentException e) {
        //            errors.add(
        //                    "Invalid subscription initial position: "
        //                            + consumerConfig.subscriptionInitialPosition());
        //        }
    }

    private void validateTransactionConfig(List<String> warnings, List<String> errors) {
        MsgArgsConfig.TransactionConfig txConfig = config.transaction();

        if (txConfig.enabled()) {
            // 验证事务超时
            if (txConfig.timeout().toSeconds() < 1) {
                warnings.add("Transaction timeout is very short: " + txConfig.timeout());
            } else if (txConfig.timeout().toSeconds() > 300) { // 5 minutes
                warnings.add("Transaction timeout is very long: " + txConfig.timeout());
            }

            // 验证缓冲区配置
            if (txConfig.bufferSnapshotSegmentSize() < 1024) {
                warnings.add(
                        "Transaction buffer snapshot segment size is very small: "
                                + txConfig.bufferSnapshotSegmentSize());
            }

            if (txConfig.bufferSnapshotMaxTransactionCount() < 1) {
                errors.add("Transaction buffer snapshot max transaction count must be at least 1");
            }
        }
    }

    private void validateMonitoringConfig(List<String> warnings, List<String> errors) {
        MsgArgsConfig.MonitoringConfig monitoringConfig = config.monitoring();

        if (monitoringConfig.metricsEnabled()) {
            if (monitoringConfig.statsInterval().toSeconds() < 1) {
                warnings.add("Stats interval is very short: " + monitoringConfig.statsInterval());
            } else if (monitoringConfig.statsInterval().toSeconds() > 3600) { // 1 hour
                warnings.add("Stats interval is very long: " + monitoringConfig.statsInterval());
            }
        }
    }

    private void validateRetryConfig(List<String> warnings, List<String> errors) {
        MsgArgsConfig.RetryConfig retryConfig = config.retry();

        if (retryConfig.maxAttempts() < 1) {
            errors.add("Retry max attempts must be at least 1");
        } else if (retryConfig.maxAttempts() > 100) {
            warnings.add("Very high retry max attempts: " + retryConfig.maxAttempts());
        }

        if (retryConfig.initialDelay().toMillis() < 100) {
            warnings.add("Retry initial delay is very short: " + retryConfig.initialDelay());
        }

        if (retryConfig.maxDelay().compareTo(retryConfig.initialDelay()) < 0) {
            errors.add("Retry max delay must be greater than or equal to initial delay");
        }

        if (retryConfig.multiplier() <= 1.0) {
            warnings.add("Retry multiplier should be greater than 1.0 for exponential backoff");
        } else if (retryConfig.multiplier() > 10.0) {
            warnings.add("Very high retry multiplier: " + retryConfig.multiplier());
        }
    }

    private void logConfigurationSummary() {
        logger.info("Pulsar Configuration Summary:");
        logger.info("  Service URL: {}", config.client().serviceUrl());
        logger.info("  TLS Enabled: {}", config.client().tls().enabled());
        logger.info(
                "  Authentication: {}",
                config.client().authToken().isPresent()
                        ? "Token"
                        : config.client().authPluginClassName().isPresent() ? "Custom" : "None");
        logger.info("  Transaction Enabled: {}", config.transaction().enabled());
        logger.info("  Metrics Enabled: {}", config.monitoring().metricsEnabled());
        logger.info("  Producer Batching: {}", config.producer().batchingEnabled());
        logger.info("  Consumer Batch Receive: {}", config.consumer().batchReceiveEnabled());
        logger.info("  Max Redeliver Count: {}", config.consumer().maxRedeliverCount());
        logger.info("  Retry Max Attempts: {}", config.retry().maxAttempts());
    }
}
