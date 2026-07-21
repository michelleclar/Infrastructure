package org.carl.infra.mq.kafka.config;

import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.SubscriptionInitialPosition;
import org.carl.infra.mq.consumer.SubscriptionType;
import org.carl.infra.mq.consumer.SubscriptionTypes;
import org.carl.infra.mq.producer.CompressionType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class KafkaConfig implements MQConfig {

    private String name;
    private KafkaClientConfig clientConfig;
    private KafkaProducerConfig producerConfig;
    private KafkaConsumerConfig consumerConfig;
    private KafkaTransactionConfig transactionConfig;
    private KafkaMonitoringConfig monitoringConfig;
    private KafkaRetryConfig retryConfig;

    // 默认构造方法
    public KafkaConfig() {
        this.clientConfig = new KafkaClientConfig();
        this.producerConfig = new KafkaProducerConfig();
        this.consumerConfig = new KafkaConsumerConfig();
        this.transactionConfig = new KafkaTransactionConfig();
        this.monitoringConfig = new KafkaMonitoringConfig();
        this.retryConfig = new KafkaRetryConfig();
    }

    // 通过 URL (bootstrapServers) 构建
    public KafkaConfig(String bootstrapServers) {
        this();
        this.clientConfig.setBootstrapServers(bootstrapServers);
    }

    // 通过 URL 和认证 Token 构建
    public KafkaConfig(String bootstrapServers, String authToken) {
        this(bootstrapServers);
        this.clientConfig.setAuthToken(authToken);
    }

    // 通过 URL、认证插件和参数构建
    public KafkaConfig(String bootstrapServers, String authPluginClassName, String authParams) {
        this(bootstrapServers);
        this.clientConfig.setAuthPluginClassName(authPluginClassName);
        this.clientConfig.setAuthParams(authParams);
    }

    // 完整参数构造方法
    public KafkaConfig(
            KafkaClientConfig clientConfig,
            KafkaProducerConfig producerConfig,
            KafkaConsumerConfig consumerConfig,
            KafkaTransactionConfig transactionConfig,
            KafkaMonitoringConfig monitoringConfig,
            KafkaRetryConfig retryConfig) {
        this.clientConfig = clientConfig;
        this.producerConfig = producerConfig;
        this.consumerConfig = consumerConfig;
        this.transactionConfig = transactionConfig;
        this.monitoringConfig = monitoringConfig;
        this.retryConfig = retryConfig;
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(this.name);
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

    public void setName(String name) {
        this.name = name;
    }

    public void setClientConfig(KafkaClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void setProducerConfig(KafkaProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
    }

    public void setConsumerConfig(KafkaConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public void setTransactionConfig(KafkaTransactionConfig transactionConfig) {
        this.transactionConfig = transactionConfig;
    }

    public void setMonitoringConfig(KafkaMonitoringConfig monitoringConfig) {
        this.monitoringConfig = monitoringConfig;
    }

    public void setRetryConfig(KafkaRetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    // ==================== 内部实现类 ====================

    public static class KafkaClientConfig implements ClientConfig {
        private String bootstrapServers = "localhost:9092";
        private String adminUrl;
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
        private KafkaTlsConfig tlsConfig = new KafkaTlsConfig();

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        @Override
        public String serviceUrl() {
            return bootstrapServers;
        }

        public void setServiceUrl(String serviceUrl) {
            this.bootstrapServers = serviceUrl;
        }

        @Override
        public Optional<String> adminUrl() {
            return Optional.ofNullable(adminUrl);
        }

        public void setAdminUrl(String adminUrl) {
            this.adminUrl = adminUrl;
        }

        @Override
        public Optional<String> authPluginClassName() {
            return Optional.ofNullable(authPluginClassName);
        }

        public void setAuthPluginClassName(String authPluginClassName) {
            this.authPluginClassName = authPluginClassName;
        }

        @Override
        public Optional<String> authParams() {
            return Optional.ofNullable(authParams);
        }

        public void setAuthParams(String authParams) {
            this.authParams = authParams;
        }

        @Override
        public Optional<String> authToken() {
            return Optional.ofNullable(authToken);
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        @Override
        public Duration operationTimeout() {
            return operationTimeout;
        }

        public void setOperationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
        }

        @Override
        public Duration connectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        @Override
        public int connectionsPerBroker() {
            return connectionsPerBroker;
        }

        public void setConnectionsPerBroker(int connectionsPerBroker) {
            this.connectionsPerBroker = connectionsPerBroker;
        }

        @Override
        public boolean tcpNoDelay() {
            return tcpNoDelay;
        }

        public void setTcpNoDelay(boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
        }

        @Override
        public Duration keepAliveInterval() {
            return keepAliveInterval;
        }

        public void setKeepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
        }

        @Override
        public long memoryLimit() {
            return memoryLimit;
        }

        public void setMemoryLimit(long memoryLimit) {
            this.memoryLimit = memoryLimit;
        }

        @Override
        public int maxLookupRequests() {
            return maxLookupRequests;
        }

        public void setMaxLookupRequests(int maxLookupRequests) {
            this.maxLookupRequests = maxLookupRequests;
        }

        @Override
        public int maxLookupRedirects() {
            return maxLookupRedirects;
        }

        public void setMaxLookupRedirects(int maxLookupRedirects) {
            this.maxLookupRedirects = maxLookupRedirects;
        }

        @Override
        public int maxConcurrentLookupRequests() {
            return maxConcurrentLookupRequests;
        }

        public void setMaxConcurrentLookupRequests(int maxConcurrentLookupRequests) {
            this.maxConcurrentLookupRequests = maxConcurrentLookupRequests;
        }

        @Override
        public TlsConfig tls() {
            return tlsConfig;
        }

        public void setTlsConfig(KafkaTlsConfig tlsConfig) {
            this.tlsConfig = tlsConfig;
        }
    }

    public static class KafkaTlsConfig implements TlsConfig {
        private boolean enabled = false;
        private String trustCertsFilePath;
        private boolean allowInsecureConnection = false;
        private boolean enableHostnameVerification = true;

        @Override
        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public Optional<String> trustCertsFilePath() {
            return Optional.ofNullable(trustCertsFilePath);
        }

        public void setTrustCertsFilePath(String trustCertsFilePath) {
            this.trustCertsFilePath = trustCertsFilePath;
        }

        @Override
        public boolean allowInsecureConnection() {
            return allowInsecureConnection;
        }

        public void setAllowInsecureConnection(boolean allowInsecureConnection) {
            this.allowInsecureConnection = allowInsecureConnection;
        }

        @Override
        public boolean enableHostnameVerification() {
            return enableHostnameVerification;
        }

        public void setEnableHostnameVerification(boolean enableHostnameVerification) {
            this.enableHostnameVerification = enableHostnameVerification;
        }
    }

    public static class KafkaProducerConfig implements ProducerConfig {
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

        public void setSendTimeout(Duration sendTimeout) {
            this.sendTimeout = sendTimeout;
        }

        @Override
        public boolean batchingEnabled() {
            return batchingEnabled;
        }

        public void setBatchingEnabled(boolean batchingEnabled) {
            this.batchingEnabled = batchingEnabled;
        }

        @Override
        public int batchingMaxMessages() {
            return batchingMaxMessages;
        }

        public void setBatchingMaxMessages(int batchingMaxMessages) {
            this.batchingMaxMessages = batchingMaxMessages;
        }

        @Override
        public Duration batchingMaxPublishDelay() {
            return batchingMaxPublishDelay;
        }

        public void setBatchingMaxPublishDelay(Duration batchingMaxPublishDelay) {
            this.batchingMaxPublishDelay = batchingMaxPublishDelay;
        }

        @Override
        public int batchingMaxBytes() {
            return batchingMaxBytes;
        }

        public void setBatchingMaxBytes(int batchingMaxBytes) {
            this.batchingMaxBytes = batchingMaxBytes;
        }

        @Override
        public int maxPendingMessages() {
            return maxPendingMessages;
        }

        public void setMaxPendingMessages(int maxPendingMessages) {
            this.maxPendingMessages = maxPendingMessages;
        }

        @Override
        public String blockIfQueueFull() {
            return blockIfQueueFull;
        }

        public void setBlockIfQueueFull(String blockIfQueueFull) {
            this.blockIfQueueFull = blockIfQueueFull;
        }

        @Override
        public CompressionType compressionType() {
            return compressionType;
        }

        public void setCompressionType(CompressionType compressionType) {
            this.compressionType = compressionType;
        }

        @Override
        public boolean chunkingEnabled() {
            return chunkingEnabled;
        }

        public void setChunkingEnabled(boolean chunkingEnabled) {
            this.chunkingEnabled = chunkingEnabled;
        }

        @Override
        public int chunkMaxMessageSize() {
            return chunkMaxMessageSize;
        }

        public void setChunkMaxMessageSize(int chunkMaxMessageSize) {
            this.chunkMaxMessageSize = chunkMaxMessageSize;
        }
    }

    public static class KafkaConsumerConfig implements ConsumerConfig {
        private Duration ackTimeout = Duration.ZERO;
        private Duration ackTimeoutTickTime = Duration.ofSeconds(1);
        private Duration negativeAckRedeliveryDelay = Duration.ofMinutes(1);
        private int receiverQueueSize = 1000;
        private int maxRedeliverCount = 3;
        private String deadLetterTopicSuffix = "-DLQ";
        private String retryTopicSuffix = "-RETRY";
        private boolean batchReceiveEnabled = false;
        private int batchReceiveMaxMessages = 100;
        private Duration batchReceiveTimeout = Duration.ofMillis(100);
        private SubscriptionInitialPosition subscriptionInitialPosition = SubscriptionInitialPosition.Latest;
        private int priority = 0;
        private boolean readCompacted = false;
        private SubscriptionType subscriptionType = SubscriptionTypes.LOAD_BALANCED;
        private boolean autoAck = false;

        @Override
        public Boolean autoAck() {
            return autoAck;
        }

        public void setAutoAck(boolean autoAck) {
            this.autoAck = autoAck;
        }

        @Override
        public Duration ackTimeout() {
            return ackTimeout;
        }

        public void setAckTimeout(Duration ackTimeout) {
            this.ackTimeout = ackTimeout;
        }

        @Override
        public Duration ackTimeoutTickTime() {
            return ackTimeoutTickTime;
        }

        public void setAckTimeoutTickTime(Duration ackTimeoutTickTime) {
            this.ackTimeoutTickTime = ackTimeoutTickTime;
        }

        @Override
        public Duration negativeAckRedeliveryDelay() {
            return negativeAckRedeliveryDelay;
        }

        public void setNegativeAckRedeliveryDelay(Duration negativeAckRedeliveryDelay) {
            this.negativeAckRedeliveryDelay = negativeAckRedeliveryDelay;
        }

        @Override
        public int receiverQueueSize() {
            return receiverQueueSize;
        }

        public void setReceiverQueueSize(int receiverQueueSize) {
            this.receiverQueueSize = receiverQueueSize;
        }

        @Override
        public int maxRedeliverCount() {
            return maxRedeliverCount;
        }

        public void setMaxRedeliverCount(int maxRedeliverCount) {
            this.maxRedeliverCount = maxRedeliverCount;
        }

        @Override
        public String deadLetterTopicSuffix() {
            return deadLetterTopicSuffix;
        }

        public void setDeadLetterTopicSuffix(String deadLetterTopicSuffix) {
            this.deadLetterTopicSuffix = deadLetterTopicSuffix;
        }

        @Override
        public String retryTopicSuffix() {
            return retryTopicSuffix;
        }

        public void setRetryTopicSuffix(String retryTopicSuffix) {
            this.retryTopicSuffix = retryTopicSuffix;
        }

        @Override
        public boolean batchReceiveEnabled() {
            return batchReceiveEnabled;
        }

        public void setBatchReceiveEnabled(boolean batchReceiveEnabled) {
            this.batchReceiveEnabled = batchReceiveEnabled;
        }

        @Override
        public int batchReceiveMaxMessages() {
            return batchReceiveMaxMessages;
        }

        public void setBatchReceiveMaxMessages(int batchReceiveMaxMessages) {
            this.batchReceiveMaxMessages = batchReceiveMaxMessages;
        }

        @Override
        public Duration batchReceiveTimeout() {
            return batchReceiveTimeout;
        }

        public void setBatchReceiveTimeout(Duration batchReceiveTimeout) {
            this.batchReceiveTimeout = batchReceiveTimeout;
        }

        @Override
        public SubscriptionInitialPosition subscriptionInitialPosition() {
            return subscriptionInitialPosition;
        }

        public void setSubscriptionInitialPosition(SubscriptionInitialPosition subscriptionInitialPosition) {
            this.subscriptionInitialPosition = subscriptionInitialPosition;
        }

        @Override
        public int priority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public boolean readCompacted() {
            return readCompacted;
        }

        public void setReadCompacted(boolean readCompacted) {
            this.readCompacted = readCompacted;
        }

        @Override
        public SubscriptionType subscriptionType() {
            return subscriptionType;
        }

        public void setSubscriptionType(SubscriptionType subscriptionType) {
            this.subscriptionType = subscriptionType;
        }
    }

    public static class KafkaTransactionConfig implements TransactionConfig {
        private boolean enabled = false;
        private String coordinatorTopic = "transaction-coordinator";
        private Duration timeout = Duration.ofMinutes(1);
        private int bufferSnapshotSegmentSize = 256;
        private Duration bufferSnapshotMinTime = Duration.ofMillis(5);
        private int bufferSnapshotMaxTransactionCount = 10;

        @Override
        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public String coordinatorTopic() {
            return coordinatorTopic;
        }

        public void setCoordinatorTopic(String coordinatorTopic) {
            this.coordinatorTopic = coordinatorTopic;
        }

        @Override
        public Duration timeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        @Override
        public int bufferSnapshotSegmentSize() {
            return bufferSnapshotSegmentSize;
        }

        public void setBufferSnapshotSegmentSize(int bufferSnapshotSegmentSize) {
            this.bufferSnapshotSegmentSize = bufferSnapshotSegmentSize;
        }

        @Override
        public Duration bufferSnapshotMinTime() {
            return bufferSnapshotMinTime;
        }

        public void setBufferSnapshotMinTime(Duration bufferSnapshotMinTime) {
            this.bufferSnapshotMinTime = bufferSnapshotMinTime;
        }

        @Override
        public int bufferSnapshotMaxTransactionCount() {
            return bufferSnapshotMaxTransactionCount;
        }

        public void setBufferSnapshotMaxTransactionCount(int bufferSnapshotMaxTransactionCount) {
            this.bufferSnapshotMaxTransactionCount = bufferSnapshotMaxTransactionCount;
        }
    }

    public static class KafkaMonitoringConfig implements MonitoringConfig {
        private boolean metricsEnabled = true;
        private Duration statsInterval = Duration.ofSeconds(60);
        private boolean topicLevelMetricsEnabled = true;
        private boolean consumerLevelMetricsEnabled = true;
        private boolean producerLevelMetricsEnabled = true;

        @Override
        public boolean metricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        @Override
        public Duration statsInterval() {
            return statsInterval;
        }

        public void setStatsInterval(Duration statsInterval) {
            this.statsInterval = statsInterval;
        }

        @Override
        public boolean topicLevelMetricsEnabled() {
            return topicLevelMetricsEnabled;
        }

        public void setTopicLevelMetricsEnabled(boolean topicLevelMetricsEnabled) {
            this.topicLevelMetricsEnabled = topicLevelMetricsEnabled;
        }

        @Override
        public boolean consumerLevelMetricsEnabled() {
            return consumerLevelMetricsEnabled;
        }

        public void setConsumerLevelMetricsEnabled(boolean consumerLevelMetricsEnabled) {
            this.consumerLevelMetricsEnabled = consumerLevelMetricsEnabled;
        }

        @Override
        public boolean producerLevelMetricsEnabled() {
            return producerLevelMetricsEnabled;
        }

        public void setProducerLevelMetricsEnabled(boolean producerLevelMetricsEnabled) {
            this.producerLevelMetricsEnabled = producerLevelMetricsEnabled;
        }
    }

    public static class KafkaRetryConfig implements RetryConfig {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(10);
        private double multiplier = 2.0;
        private List<String> retryableExceptions;

        @Override
        public int maxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        @Override
        public Duration initialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        @Override
        public Duration maxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }

        @Override
        public double multiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public Optional<List<String>> retryableExceptions() {
            return Optional.ofNullable(retryableExceptions);
        }

        public void setRetryableExceptions(List<String> retryableExceptions) {
            this.retryableExceptions = retryableExceptions;
        }
    }
}
