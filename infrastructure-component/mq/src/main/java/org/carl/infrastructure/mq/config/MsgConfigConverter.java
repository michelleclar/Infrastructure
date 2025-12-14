package org.carl.infrastructure.mq.config;

import org.carl.infrastructure.mq.pulsar.config.PulsarConfig;

/** MsgArgsConfig 到 PulsarMsgConfig 的转换器 将 Quarkus ConfigMapping 配置对象转换为可变的 MsgConfig 实现 */
public class MsgConfigConverter {

    /**
     * 将 MsgArgsConfig 转换为 PulsarMsgConfig
     *
     * @param argsConfig Quarkus ConfigMapping 配置对象
     * @return PulsarMsgConfig 可变配置对象
     */
    public static PulsarConfig convert(MsgArgsConfig argsConfig) {
        if (argsConfig == null) {
            throw new IllegalArgumentException("MsgArgsConfig cannot be null");
        }

        PulsarConfig msgConfig = new PulsarConfig();

        // 转换客户端配置
        msgConfig.setClientConfig(convertClientConfig(argsConfig.client()));

        // 转换生产者配置
        msgConfig.setProducerConfig(convertProducerConfig(argsConfig.producer()));

        // 转换消费者配置
        msgConfig.setConsumerConfig(convertConsumerConfig(argsConfig.consumer()));

        // 转换事务配置
        msgConfig.setTransactionConfig(convertTransactionConfig(argsConfig.transaction()));

        // 转换监控配置
        msgConfig.setMonitoringConfig(convertMonitoringConfig(argsConfig.monitoring()));

        // 转换重试配置
        msgConfig.setRetryConfig(convertRetryConfig(argsConfig.retry()));

        return msgConfig;
    }

    /** 转换客户端配置 */
    private static PulsarConfig.PulsarClientConfig convertClientConfig(
            MsgArgsConfig.ClientConfig source) {
        PulsarConfig.PulsarClientConfig target = new PulsarConfig.PulsarClientConfig();

        target.setServiceUrl(source.serviceUrl());
        source.authPluginClassName().ifPresent(target::setAuthPluginClassName);
        source.authParams().ifPresent(target::setAuthParams);
        source.authToken().ifPresent(target::setAuthToken);
        target.setOperationTimeout(source.operationTimeout());
        target.setConnectionTimeout(source.connectionTimeout());
        target.setConnectionsPerBroker(source.connectionsPerBroker());
        target.setTcpNoDelay(source.tcpNoDelay());
        target.setKeepAliveInterval(source.keepAliveInterval());
        target.setMemoryLimit(source.memoryLimit());
        target.setMaxLookupRequests(source.maxLookupRequests());
        target.setMaxLookupRedirects(source.maxLookupRedirects());
        target.setMaxConcurrentLookupRequests(source.maxConcurrentLookupRequests());

        // 转换 TLS 配置
        target.setTlsConfig(convertTlsConfig(source.tls()));

        return target;
    }

    /** 转换 TLS 配置 */
    private static PulsarConfig.PulsarTlsConfig convertTlsConfig(MsgArgsConfig.TlsConfig source) {
        PulsarConfig.PulsarTlsConfig target = new PulsarConfig.PulsarTlsConfig();

        target.setEnabled(source.enabled());
        source.trustCertsFilePath().ifPresent(target::setTrustCertsFilePath);
        target.setAllowInsecureConnection(source.allowInsecureConnection());
        target.setEnableHostnameVerification(source.enableHostnameVerification());

        return target;
    }

    /** 转换生产者配置 */
    private static PulsarConfig.PulsarProducerConfig convertProducerConfig(
            MsgArgsConfig.ProducerConfig source) {
        PulsarConfig.PulsarProducerConfig target = new PulsarConfig.PulsarProducerConfig();

        target.setSendTimeout(source.sendTimeout());
        target.setBatchingEnabled(source.batchingEnabled());
        target.setBatchingMaxMessages(source.batchingMaxMessages());
        target.setBatchingMaxPublishDelay(source.batchingMaxPublishDelay());
        target.setBatchingMaxBytes(source.batchingMaxBytes());
        target.setMaxPendingMessages(source.maxPendingMessages());
        target.setBlockIfQueueFull(source.blockIfQueueFull());

        // 转换压缩类型枚举
        target.setCompressionType(source.compressionType());

        target.setChunkingEnabled(source.chunkingEnabled());
        target.setChunkMaxMessageSize(source.chunkMaxMessageSize());

        return target;
    }

    /** 转换消费者配置 */
    private static PulsarConfig.PulsarConsumerConfig convertConsumerConfig(
            MsgArgsConfig.ConsumerConfig source) {
        PulsarConfig.PulsarConsumerConfig target = new PulsarConfig.PulsarConsumerConfig();

        target.setAckTimeout(source.ackTimeout());
        target.setAckTimeoutTickTime(source.ackTimeoutTickTime());
        target.setNegativeAckRedeliveryDelay(source.negativeAckRedeliveryDelay());
        target.setReceiverQueueSize(source.receiverQueueSize());
        target.setMaxRedeliverCount(source.maxRedeliverCount());
        target.setDeadLetterTopicSuffix(source.deadLetterTopicSuffix());
        target.setRetryTopicSuffix(source.retryTopicSuffix());
        target.setBatchReceiveEnabled(source.batchReceiveEnabled());
        target.setBatchReceiveMaxMessages(source.batchReceiveMaxMessages());
        target.setBatchReceiveTimeout(source.batchReceiveTimeout());
        target.setAutoAck(source.autoAck());
        // 转换订阅初始位置枚举
        target.setSubscriptionInitialPosition(source.subscriptionInitialPosition());

        target.setPriority(source.priority());
        target.setReadCompacted(source.readCompacted());

        // 转换订阅类型枚举
        target.setSubscriptionType(source.subscriptionType());

        return target;
    }

    /** 转换事务配置 */
    private static PulsarConfig.PulsarTransactionConfig convertTransactionConfig(
            MsgArgsConfig.TransactionConfig source) {
        PulsarConfig.PulsarTransactionConfig target = new PulsarConfig.PulsarTransactionConfig();

        target.setEnabled(source.enabled());
        target.setCoordinatorTopic(source.coordinatorTopic());
        target.setTimeout(source.timeout());
        target.setBufferSnapshotSegmentSize(source.bufferSnapshotSegmentSize());
        target.setBufferSnapshotMinTime(source.bufferSnapshotMinTime());
        target.setBufferSnapshotMaxTransactionCount(source.bufferSnapshotMaxTransactionCount());

        return target;
    }

    /** 转换监控配置 */
    private static PulsarConfig.PulsarMonitoringConfig convertMonitoringConfig(
            MsgArgsConfig.MonitoringConfig source) {
        PulsarConfig.PulsarMonitoringConfig target = new PulsarConfig.PulsarMonitoringConfig();

        target.setMetricsEnabled(source.metricsEnabled());
        target.setStatsInterval(source.statsInterval());
        target.setTopicLevelMetricsEnabled(source.topicLevelMetricsEnabled());
        target.setConsumerLevelMetricsEnabled(source.consumerLevelMetricsEnabled());
        target.setProducerLevelMetricsEnabled(source.producerLevelMetricsEnabled());

        return target;
    }

    /** 转换重试配置 */
    private static PulsarConfig.PulsarRetryConfig convertRetryConfig(
            MsgArgsConfig.RetryConfig source) {
        PulsarConfig.PulsarRetryConfig target = new PulsarConfig.PulsarRetryConfig();

        target.setMaxAttempts(source.maxAttempts());
        target.setInitialDelay(source.initialDelay());
        target.setMaxDelay(source.maxDelay());
        target.setMultiplier(source.multiplier());
        source.retryableExceptions().ifPresent(target::setRetryableExceptions);

        return target;
    }
}
