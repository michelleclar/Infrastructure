package org.carl.infra.mq.kafka.config;

import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.config.MQConfig;

import java.util.ArrayList;
import java.util.List;

public class KafkaConfigValidator {

    private static final ILogger logger = LoggerFactory.getLogger(KafkaConfigValidator.class);

    public static void validate(MQConfig config) {
        logger.info("Validating Kafka configuration...");

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        validateClientConfig(config.client(), warnings, errors);
        validateProducerConfig(config.producer(), warnings, errors);
        validateConsumerConfig(config.consumer(), warnings, errors);
        validateTransactionConfig(config.transaction(), warnings, errors);
        validateMonitoringConfig(config.monitoring(), warnings, errors);
        validateRetryConfig(config.retry(), warnings, errors);

        // 输出警告
        if (!warnings.isEmpty()) {
            logger.warn("Kafka configuration warnings:");
            warnings.forEach(warning -> logger.warn("  - {}", warning));
        }

        // 输出错误并抛出异常
        if (!errors.isEmpty()) {
            logger.error("Kafka configuration errors:");
            errors.forEach(error -> logger.error("  - {}", error));
            throw new IllegalArgumentException(
                    "Invalid Kafka configuration. Please check the errors above.");
        }

        logger.info("Kafka configuration validation completed successfully");
        logConfigurationSummary(config);
    }

    private static void validateClientConfig(
            MQConfig.ClientConfig clientConfig, List<String> warnings, List<String> errors) {

        // 验证服务地址 (bootstrapServers)
        if (clientConfig.serviceUrl() == null || clientConfig.serviceUrl().trim().isEmpty()) {
            errors.add("Client service URL (bootstrap.servers) cannot be empty");
        }

        // 验证超时配置
        if (clientConfig.operationTimeout().toSeconds() < 1) {
            warnings.add("Operation timeout is very short: " + clientConfig.operationTimeout());
        }

        if (clientConfig.connectionTimeout().toSeconds() < 1) {
            warnings.add("Connection timeout is very short: " + clientConfig.connectionTimeout());
        }

        // 验证 TLS 配置
        if (clientConfig.tls().enabled()) {
            if (clientConfig.tls().allowInsecureConnection()
                    && clientConfig.tls().enableHostnameVerification()) {
                warnings.add(
                        "Hostname verification is enabled but insecure connections are allowed");
            }
        }
    }

    private static void validateProducerConfig(
            MQConfig.ProducerConfig producerConfig, List<String> warnings, List<String> errors) {

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
        }
    }

    private static void validateConsumerConfig(
            MQConfig.ConsumerConfig consumerConfig, List<String> warnings, List<String> errors) {

        // 验证接收队列大小
        if (consumerConfig.receiverQueueSize() < 1) {
            errors.add("Receiver queue size must be at least 1");
        }

        // 验证重试配置
        if (consumerConfig.maxRedeliverCount() < 0) {
            errors.add("Max redeliver count cannot be negative");
        }
    }

    private static void validateTransactionConfig(
            MQConfig.TransactionConfig txConfig, List<String> warnings, List<String> errors) {

        if (txConfig.enabled()) {
            // 验证事务超时
            if (txConfig.timeout().toSeconds() < 1) {
                warnings.add("Transaction timeout is very short: " + txConfig.timeout());
            }
        }
    }

    private static void validateMonitoringConfig(
            MQConfig.MonitoringConfig monitoringConfig,
            List<String> warnings,
            List<String> errors) {

        if (monitoringConfig.metricsEnabled()) {
            if (monitoringConfig.statsInterval().toSeconds() < 1) {
                warnings.add("Stats interval is very short: " + monitoringConfig.statsInterval());
            }
        }
    }

    private static void validateRetryConfig(
            MQConfig.RetryConfig retryConfig, List<String> warnings, List<String> errors) {

        if (retryConfig.maxAttempts() < 1) {
            errors.add("Retry max attempts must be at least 1");
        }

        if (retryConfig.initialDelay().toMillis() < 100) {
            warnings.add("Retry initial delay is very short: " + retryConfig.initialDelay());
        }

        if (retryConfig.maxDelay().compareTo(retryConfig.initialDelay()) < 0) {
            errors.add("Retry max delay must be greater than or equal to initial delay");
        }

        if (retryConfig.multiplier() <= 1.0) {
            warnings.add("Retry multiplier should be greater than 1.0 for exponential backoff");
        }
    }

    private static void logConfigurationSummary(MQConfig config) {
        logger.info("Kafka Configuration Summary:");
        logger.info("  Bootstrap Servers: {}", config.client().serviceUrl());
        logger.info("  TLS Enabled: {}", config.client().tls().enabled());
        logger.info("  Transaction Enabled: {}", config.transaction().enabled());
        logger.info("  Metrics Enabled: {}", config.monitoring().metricsEnabled());
        logger.info("  Producer Batching: {}", config.producer().batchingEnabled());
        logger.info("  Consumer Batch Receive: {}", config.consumer().batchReceiveEnabled());
    }
}
