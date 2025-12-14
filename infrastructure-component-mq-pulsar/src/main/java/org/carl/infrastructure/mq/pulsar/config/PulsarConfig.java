package org.carl.infrastructure.mq.pulsar.config;

import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.consumer.SubscriptionInitialPosition;
import org.carl.infrastructure.mq.consumer.SubscriptionType;
import org.carl.infrastructure.mq.producer.CompressionType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class PulsarConfig implements MQConfig {

    private PulsarClientConfig clientConfig;
    private PulsarProducerConfig producerConfig;
    private PulsarConsumerConfig consumerConfig;
    private PulsarTransactionConfig transactionConfig;
    private PulsarMonitoringConfig monitoringConfig;
    private PulsarRetryConfig retryConfig;

    // 默认构造方法
    public PulsarConfig() {
        this.clientConfig = new PulsarClientConfig();
        this.producerConfig = new PulsarProducerConfig();
        this.consumerConfig = new PulsarConsumerConfig();
        this.transactionConfig = new PulsarTransactionConfig();
        this.monitoringConfig = new PulsarMonitoringConfig();
        this.retryConfig = new PulsarRetryConfig();
    }

    // 通过 URL 构建
    public PulsarConfig(String serviceUrl) {
        this();
        this.clientConfig.setServiceUrl(serviceUrl);
    }

    // 通过 URL 和认证 Token 构建
    public PulsarConfig(String serviceUrl, String authToken) {
        this(serviceUrl);
        this.clientConfig.setAuthToken(authToken);
    }

    // 通过 URL、认证插件和参数构建
    public PulsarConfig(String serviceUrl, String authPluginClassName, String authParams) {
        this(serviceUrl);
        this.clientConfig.setAuthPluginClassName(authPluginClassName);
        this.clientConfig.setAuthParams(authParams);
    }

    // 完整参数构造方法
    public PulsarConfig(
            PulsarClientConfig clientConfig,
            PulsarProducerConfig producerConfig,
            PulsarConsumerConfig consumerConfig,
            PulsarTransactionConfig transactionConfig,
            PulsarMonitoringConfig monitoringConfig,
            PulsarRetryConfig retryConfig) {
        this.clientConfig = clientConfig;
        this.producerConfig = producerConfig;
        this.consumerConfig = consumerConfig;
        this.transactionConfig = transactionConfig;
        this.monitoringConfig = monitoringConfig;
        this.retryConfig = retryConfig;
    }

    @Override
    public ClientConfig client() {
        return clientConfig;
    }

    @Override
    public ProducerConfig producer() {
        return producerConfig;
    }

    @Override
    public ConsumerConfig consumer() {
        return consumerConfig;
    }

    @Override
    public TransactionConfig transaction() {
        return transactionConfig;
    }

    @Override
    public MonitoringConfig monitoring() {
        return monitoringConfig;
    }

    @Override
    public RetryConfig retry() {
        return retryConfig;
    }

    public void setClientConfig(PulsarClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void setProducerConfig(PulsarProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
    }

    public void setConsumerConfig(PulsarConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public void setTransactionConfig(PulsarTransactionConfig transactionConfig) {
        this.transactionConfig = transactionConfig;
    }

    public void setMonitoringConfig(PulsarMonitoringConfig monitoringConfig) {
        this.monitoringConfig = monitoringConfig;
    }

    public void setRetryConfig(PulsarRetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    // ==================== 内部实现类 ====================

    public static class PulsarClientConfig implements ClientConfig {
        private String serviceUrl = "pulsar://localhost:6650";
        private String authPluginClassName;
        private String authParams;
        private String authToken;
        private Duration operationTimeout = Duration.ofSeconds(30);
        private Duration connectionTimeout = Duration.ofSeconds(10);
        private int connectionsPerBroker = 1;
        private boolean tcpNoDelay = true;
        private Duration keepAliveInterval = Duration.ofSeconds(30);
        private long memoryLimit = 64 * 1024 * 1024; // 64MB
        private int maxLookupRequests = 50000;
        private int maxLookupRedirects = 20;
        private int maxConcurrentLookupRequests = 5000;
        private PulsarTlsConfig tlsConfig = new PulsarTlsConfig();

        @Override
        public String serviceUrl() {
            return serviceUrl;
        }

        @Override
        public Optional<String> authPluginClassName() {
            return Optional.ofNullable(authPluginClassName);
        }

        @Override
        public Optional<String> authParams() {
            return Optional.ofNullable(authParams);
        }

        @Override
        public Optional<String> authToken() {
            return Optional.ofNullable(authToken);
        }

        @Override
        public Duration operationTimeout() {
            return operationTimeout;
        }

        @Override
        public Duration connectionTimeout() {
            return connectionTimeout;
        }

        @Override
        public int connectionsPerBroker() {
            return connectionsPerBroker;
        }

        @Override
        public boolean tcpNoDelay() {
            return tcpNoDelay;
        }

        @Override
        public Duration keepAliveInterval() {
            return keepAliveInterval;
        }

        @Override
        public long memoryLimit() {
            return memoryLimit;
        }

        @Override
        public int maxLookupRequests() {
            return maxLookupRequests;
        }

        @Override
        public int maxLookupRedirects() {
            return maxLookupRedirects;
        }

        @Override
        public int maxConcurrentLookupRequests() {
            return maxConcurrentLookupRequests;
        }

        @Override
        public TlsConfig tls() {
            return tlsConfig;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public void setAuthPluginClassName(String authPluginClassName) {
            this.authPluginClassName = authPluginClassName;
        }

        public void setAuthParams(String authParams) {
            this.authParams = authParams;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public void setOperationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public void setConnectionsPerBroker(int connectionsPerBroker) {
            this.connectionsPerBroker = connectionsPerBroker;
        }

        public void setTcpNoDelay(boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
        }

        public void setKeepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
        }

        public void setMemoryLimit(long memoryLimit) {
            this.memoryLimit = memoryLimit;
        }

        public void setMaxLookupRequests(int maxLookupRequests) {
            this.maxLookupRequests = maxLookupRequests;
        }

        public void setMaxLookupRedirects(int maxLookupRedirects) {
            this.maxLookupRedirects = maxLookupRedirects;
        }

        public void setMaxConcurrentLookupRequests(int maxConcurrentLookupRequests) {
            this.maxConcurrentLookupRequests = maxConcurrentLookupRequests;
        }

        public void setTlsConfig(PulsarTlsConfig tlsConfig) {
            this.tlsConfig = tlsConfig;
        }
    }

    public static class PulsarTlsConfig implements TlsConfig {
        private boolean enabled = false;
        private String trustCertsFilePath;
        private boolean allowInsecureConnection = false;
        private boolean enableHostnameVerification = true;

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public Optional<String> trustCertsFilePath() {
            return Optional.ofNullable(trustCertsFilePath);
        }

        @Override
        public boolean allowInsecureConnection() {
            return allowInsecureConnection;
        }

        @Override
        public boolean enableHostnameVerification() {
            return enableHostnameVerification;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setTrustCertsFilePath(String trustCertsFilePath) {
            this.trustCertsFilePath = trustCertsFilePath;
        }

        public void setEnableHostnameVerification(boolean enableHostnameVerification) {
            this.enableHostnameVerification = enableHostnameVerification;
        }

        public void setAllowInsecureConnection(boolean allowInsecureConnection) {
            this.allowInsecureConnection = allowInsecureConnection;
        }
    }

    public static class PulsarProducerConfig implements ProducerConfig {
        private Duration sendTimeout = Duration.ofSeconds(30);
        private boolean batchingEnabled = true;
        private int batchingMaxMessages = 1000;
        private Duration batchingMaxPublishDelay = Duration.ofMillis(1);
        private int batchingMaxBytes = 128 * 1024; // 128KB
        private int maxPendingMessages = 1000;
        private String blockIfQueueFull = "true";
        private CompressionType compressionType = CompressionType.LZ4;
        private boolean chunkingEnabled = false;
        private int chunkMaxMessageSize = 5 * 1024 * 1024; // 5MB

        @Override
        public Duration sendTimeout() {
            return sendTimeout;
        }

        @Override
        public boolean batchingEnabled() {
            return batchingEnabled;
        }

        @Override
        public int batchingMaxMessages() {
            return batchingMaxMessages;
        }

        @Override
        public Duration batchingMaxPublishDelay() {
            return batchingMaxPublishDelay;
        }

        @Override
        public int batchingMaxBytes() {
            return batchingMaxBytes;
        }

        @Override
        public int maxPendingMessages() {
            return maxPendingMessages;
        }

        @Override
        public String blockIfQueueFull() {
            return blockIfQueueFull;
        }

        @Override
        public CompressionType compressionType() {
            return compressionType;
        }

        @Override
        public boolean chunkingEnabled() {
            return chunkingEnabled;
        }

        @Override
        public int chunkMaxMessageSize() {
            return chunkMaxMessageSize;
        }

        public void setSendTimeout(Duration sendTimeout) {
            this.sendTimeout = sendTimeout;
        }

        public void setBatchingEnabled(boolean batchingEnabled) {
            this.batchingEnabled = batchingEnabled;
        }

        public void setBatchingMaxMessages(int batchingMaxMessages) {
            this.batchingMaxMessages = batchingMaxMessages;
        }

        public void setBatchingMaxPublishDelay(Duration batchingMaxPublishDelay) {
            this.batchingMaxPublishDelay = batchingMaxPublishDelay;
        }

        public void setBatchingMaxBytes(int batchingMaxBytes) {
            this.batchingMaxBytes = batchingMaxBytes;
        }

        public void setMaxPendingMessages(int maxPendingMessages) {
            this.maxPendingMessages = maxPendingMessages;
        }

        public void setBlockIfQueueFull(String blockIfQueueFull) {
            this.blockIfQueueFull = blockIfQueueFull;
        }

        public void setCompressionType(CompressionType compressionType) {
            this.compressionType = compressionType;
        }

        public void setChunkingEnabled(boolean chunkingEnabled) {
            this.chunkingEnabled = chunkingEnabled;
        }

        public void setChunkMaxMessageSize(int chunkMaxMessageSize) {
            this.chunkMaxMessageSize = chunkMaxMessageSize;
        }
    }

    public static class PulsarConsumerConfig implements ConsumerConfig {
        private Duration ackTimeout = Duration.ZERO; // 禁用确认超时
        private Duration ackTimeoutTickTime = Duration.ofSeconds(1);
        private Duration negativeAckRedeliveryDelay = Duration.ofMinutes(1);
        private int receiverQueueSize = 1000;
        private int maxRedeliverCount = 3;
        private String deadLetterTopicSuffix = "-DLQ";
        private String retryTopicSuffix = "-RETRY";
        private boolean batchReceiveEnabled = false;
        private int batchReceiveMaxMessages = 100;
        private Duration batchReceiveTimeout = Duration.ofMillis(100);
        private SubscriptionInitialPosition subscriptionInitialPosition =
                SubscriptionInitialPosition.Latest;
        private int priority = 0;
        private boolean readCompacted = false;
        private SubscriptionType subscriptionType = SubscriptionType.EXCLUSIVE;
        private boolean autoAck = false;

        @Override
        public Duration ackTimeout() {
            return ackTimeout;
        }

        @Override
        public Duration ackTimeoutTickTime() {
            return ackTimeoutTickTime;
        }

        @Override
        public Duration negativeAckRedeliveryDelay() {
            return negativeAckRedeliveryDelay;
        }

        @Override
        public int receiverQueueSize() {
            return receiverQueueSize;
        }

        @Override
        public int maxRedeliverCount() {
            return maxRedeliverCount;
        }

        @Override
        public String deadLetterTopicSuffix() {
            return deadLetterTopicSuffix;
        }

        @Override
        public String retryTopicSuffix() {
            return retryTopicSuffix;
        }

        @Override
        public boolean batchReceiveEnabled() {
            return batchReceiveEnabled;
        }

        @Override
        public int batchReceiveMaxMessages() {
            return batchReceiveMaxMessages;
        }

        @Override
        public Duration batchReceiveTimeout() {
            return batchReceiveTimeout;
        }

        @Override
        public SubscriptionInitialPosition subscriptionInitialPosition() {
            return subscriptionInitialPosition;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean readCompacted() {
            return readCompacted;
        }

        @Override
        public SubscriptionType subscriptionType() {
            return subscriptionType;
        }

        public void setAckTimeout(Duration ackTimeout) {
            this.ackTimeout = ackTimeout;
        }

        public void setAckTimeoutTickTime(Duration ackTimeoutTickTime) {
            this.ackTimeoutTickTime = ackTimeoutTickTime;
        }

        public void setNegativeAckRedeliveryDelay(Duration negativeAckRedeliveryDelay) {
            this.negativeAckRedeliveryDelay = negativeAckRedeliveryDelay;
        }

        public void setReceiverQueueSize(int receiverQueueSize) {
            this.receiverQueueSize = receiverQueueSize;
        }

        public void setMaxRedeliverCount(int maxRedeliverCount) {
            this.maxRedeliverCount = maxRedeliverCount;
        }

        public void setDeadLetterTopicSuffix(String deadLetterTopicSuffix) {
            this.deadLetterTopicSuffix = deadLetterTopicSuffix;
        }

        public void setRetryTopicSuffix(String retryTopicSuffix) {
            this.retryTopicSuffix = retryTopicSuffix;
        }

        public void setBatchReceiveEnabled(boolean batchReceiveEnabled) {
            this.batchReceiveEnabled = batchReceiveEnabled;
        }


        public void setBatchReceiveMaxMessages(int batchReceiveMaxMessages) {
            this.batchReceiveMaxMessages = batchReceiveMaxMessages;
        }

        public void setBatchReceiveTimeout(Duration batchReceiveTimeout) {
            this.batchReceiveTimeout = batchReceiveTimeout;
        }

        public void setSubscriptionInitialPosition(
                SubscriptionInitialPosition subscriptionInitialPosition) {
            this.subscriptionInitialPosition = subscriptionInitialPosition;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public void setReadCompacted(boolean readCompacted) {
            this.readCompacted = readCompacted;
        }

        public void setSubscriptionType(SubscriptionType subscriptionType) {
            this.subscriptionType = subscriptionType;
        }

        public void setAutoAck(boolean autoAck) {
            this.autoAck = autoAck;
        }
    }

    public static class PulsarTransactionConfig implements TransactionConfig {
        private boolean enabled = false;
        private String coordinatorTopic = "persistent://public/default/transaction-coordinator";
        private Duration timeout = Duration.ofMinutes(1);
        private int bufferSnapshotSegmentSize = 256;
        private Duration bufferSnapshotMinTime = Duration.ofMillis(5);
        private int bufferSnapshotMaxTransactionCount = 10;

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public String coordinatorTopic() {
            return coordinatorTopic;
        }

        @Override
        public Duration timeout() {
            return timeout;
        }

        @Override
        public int bufferSnapshotSegmentSize() {
            return bufferSnapshotSegmentSize;
        }

        @Override
        public Duration bufferSnapshotMinTime() {
            return bufferSnapshotMinTime;
        }

        @Override
        public int bufferSnapshotMaxTransactionCount() {
            return bufferSnapshotMaxTransactionCount;
        }

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCoordinatorTopic() {
            return coordinatorTopic;
        }

        public void setCoordinatorTopic(String coordinatorTopic) {
            this.coordinatorTopic = coordinatorTopic;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getBufferSnapshotSegmentSize() {
            return bufferSnapshotSegmentSize;
        }

        public void setBufferSnapshotSegmentSize(int bufferSnapshotSegmentSize) {
            this.bufferSnapshotSegmentSize = bufferSnapshotSegmentSize;
        }

        public Duration getBufferSnapshotMinTime() {
            return bufferSnapshotMinTime;
        }

        public void setBufferSnapshotMinTime(Duration bufferSnapshotMinTime) {
            this.bufferSnapshotMinTime = bufferSnapshotMinTime;
        }

        public int getBufferSnapshotMaxTransactionCount() {
            return bufferSnapshotMaxTransactionCount;
        }

        public void setBufferSnapshotMaxTransactionCount(int bufferSnapshotMaxTransactionCount) {
            this.bufferSnapshotMaxTransactionCount = bufferSnapshotMaxTransactionCount;
        }
    }

    public static class PulsarMonitoringConfig implements MonitoringConfig {
        private boolean metricsEnabled = true;
        private Duration statsInterval = Duration.ofSeconds(60);
        private boolean topicLevelMetricsEnabled = true;
        private boolean consumerLevelMetricsEnabled = true;
        private boolean producerLevelMetricsEnabled = true;

        @Override
        public boolean metricsEnabled() {
            return metricsEnabled;
        }

        @Override
        public Duration statsInterval() {
            return statsInterval;
        }

        @Override
        public boolean topicLevelMetricsEnabled() {
            return topicLevelMetricsEnabled;
        }

        @Override
        public boolean consumerLevelMetricsEnabled() {
            return consumerLevelMetricsEnabled;
        }

        @Override
        public boolean producerLevelMetricsEnabled() {
            return producerLevelMetricsEnabled;
        }

        // Getters and Setters
        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public Duration getStatsInterval() {
            return statsInterval;
        }

        public void setStatsInterval(Duration statsInterval) {
            this.statsInterval = statsInterval;
        }

        public boolean isTopicLevelMetricsEnabled() {
            return topicLevelMetricsEnabled;
        }

        public void setTopicLevelMetricsEnabled(boolean topicLevelMetricsEnabled) {
            this.topicLevelMetricsEnabled = topicLevelMetricsEnabled;
        }

        public boolean isConsumerLevelMetricsEnabled() {
            return consumerLevelMetricsEnabled;
        }

        public void setConsumerLevelMetricsEnabled(boolean consumerLevelMetricsEnabled) {
            this.consumerLevelMetricsEnabled = consumerLevelMetricsEnabled;
        }

        public boolean isProducerLevelMetricsEnabled() {
            return producerLevelMetricsEnabled;
        }

        public void setProducerLevelMetricsEnabled(boolean producerLevelMetricsEnabled) {
            this.producerLevelMetricsEnabled = producerLevelMetricsEnabled;
        }
    }

    public static class PulsarRetryConfig implements RetryConfig {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(10);
        private double multiplier = 2.0;
        private Optional<List<String>> retryableExceptions = Optional.empty();

        @Override
        public int maxAttempts() {
            return maxAttempts;
        }

        @Override
        public Duration initialDelay() {
            return initialDelay;
        }

        @Override
        public Duration maxDelay() {
            return maxDelay;
        }

        @Override
        public double multiplier() {
            return multiplier;
        }

        @Override
        public Optional<List<String>> retryableExceptions() {
            return retryableExceptions;
        }

        // Getters and Setters
        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public Optional<List<String>> getRetryableExceptions() {
            return retryableExceptions;
        }

        public void setRetryableExceptions(List<String> retryableExceptions) {
            this.retryableExceptions = Optional.ofNullable(retryableExceptions);
        }
    }
}
