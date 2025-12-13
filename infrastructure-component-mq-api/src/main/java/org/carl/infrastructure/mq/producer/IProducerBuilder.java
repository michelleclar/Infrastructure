package org.carl.infrastructure.mq.producer;

import org.carl.infrastructure.mq.common.ex.ProducerException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface IProducerBuilder<T> {

    @Deprecated
    IProducer<T> create() throws ProducerException;

    IProducer<T> create(String topicName) throws ProducerException;

    CompletableFuture<IProducer<T>> createAsync();

    IProducerBuilder<T> loadConf(Map<String, Object> config);

    IProducerBuilder<T> clone();

    @Deprecated
    IProducerBuilder<T> topic(String topicName);

    IProducerBuilder<T> producerName(String producerName);

    IProducerBuilder<T> accessMode(ProducerAccessMode accessMode);

    IProducerBuilder<T> sendTimeout(int sendTimeout, TimeUnit unit);

    IProducerBuilder<T> maxPendingMessages(int maxPendingMessages);

    IProducerBuilder<T> blockIfQueueFull(boolean blockIfQueueFull);

    IProducerBuilder<T> messageRoutingMode(MessageRoutingMode messageRoutingMode);

    IProducerBuilder<T> hashingScheme(HashingScheme hashingScheme);

    IProducerBuilder<T> compressionType(CompressionType compressionType);

    //    /**
    //     * Set a custom message routing policy by passing an implementation of MessageRouter.
    //     *
    //     * @return the producer builder instance
    //     */
    //    IProducerBuilder<T> messageRouter(MessageRouter messageRouter);

    IProducerBuilder<T> enableBatching(boolean enableBatching);

    IProducerBuilder<T> enableChunking(boolean enableChunking);

    IProducerBuilder<T> chunkMaxMessageSize(int chunkMaxMessageSize);

    //
    //    /**
    //     * Sets a {@link CryptoKeyReader}.
    //     *
    //     * <p>Configure the key reader to be used to encrypt the message payloads.
    //     *
    //     * @param cryptoKeyReader CryptoKeyReader object
    //     * @return the producer builder instance
    //     */
    //    IProducerBuilder<T> cryptoKeyReader(CryptoKeyReader cryptoKeyReader);

    //    /**
    //     * Sets the default implementation of {@link CryptoKeyReader}.
    //     *
    //     * <p>Configure the key reader to be used to encrypt the message payloads.
    //     *
    //     * @param publicKey the public key that is always used to encrypt message payloads.
    //     * @return the producer builder instance
    //     * @since 2.8.0
    //     */
    //    IProducerBuilder<T> defaultCryptoKeyReader(String publicKey);
    //
    //    /**
    //     * Sets the default implementation of {@link CryptoKeyReader}.
    //     *
    //     * <p>Configure the key reader to be used to encrypt the message payloads.
    //     *
    //     * @param publicKeys the map of public key names and their URIs used to encrypt message
    //     *     payloads.
    //     * @return the producer builder instance
    //     * @since 2.8.0
    //     */
    //    IProducerBuilder<T> defaultCryptoKeyReader(Map<String, String> publicKeys);
    //
    //    /**
    //     * Sets a {@link MessageCrypto}.
    //     *
    //     * <p>Contains methods to encrypt/decrypt messages for end-to-end encryption.
    //     *
    //     * @param messageCrypto MessageCrypto object
    //     * @return the producer builder instance
    //     */
    //    IProducerBuilder<T> messageCrypto(MessageCrypto messageCrypto);

    //    /**
    //     * Add public encryption key, used by producer to encrypt the data key.
    //     *
    //     * <p>At the time of producer creation, the Pulsar client checks if there are keys added
    // to
    //     * encryptionKeys. If keys are found, a callback {@link
    // CryptoKeyReader#getPrivateKey(String,
    //     * Map)} and {@link CryptoKeyReader#getPublicKey(String, Map)} is invoked against each key
    // to
    //     * load the values of the key.
    //     *
    //     * <p>Application should implement this callback to return the key in pkcs8 format. If
    //     * compression is enabled, message is encrypted after compression. If batch messaging is
    //     * enabled, the batched message is encrypted.
    //     *
    //     * @param key the name of the encryption key in the key store
    //     * @return the producer builder instance
    //     */
    //    IProducerBuilder<T> addEncryptionKey(String key);

    //    /**
    //     * Set the ProducerCryptoFailureAction to the value specified.
    //     *
    //     * @param action the action the producer will take in case of encryption failures
    //     * @return the producer builder instance
    //     */
    //    IProducerBuilder<T> cryptoFailureAction(ProducerCryptoFailureAction action);

    IProducerBuilder<T> batchingMaxPublishDelay(long batchDelay, TimeUnit timeUnit);

    IProducerBuilder<T> roundRobinRouterBatchingPartitionSwitchFrequency(int frequency);

    IProducerBuilder<T> batchingMaxMessages(int batchMessagesMaxMessagesPerBatch);

    IProducerBuilder<T> batchingMaxBytes(int batchingMaxBytes);

    //
    //    /**
    //     * Set the batcher builder {@link BatcherBuilder} of the producer. Producer will use the
    // batcher
    //     * builder to build a batch message container.This is only be used when batching is
    // enabled.
    //     *
    //     * @param batcherBuilder batcher builder
    //     * @return the producer builder instance
    //     */
    //    IProducerBuilder<T> batcherBuilder(BatcherBuilder batcherBuilder);

    IProducerBuilder<T> initialSequenceId(long initialSequenceId);

    IProducerBuilder<T> property(String key, String value);

    IProducerBuilder<T> properties(Map<String, String> properties);

    //
    //    /**
    //     * Add a set of {@link org.apache.pulsar.client.api.interceptor.ProducerInterceptor} to
    // the
    //     * producer.
    //     *
    //     * <p>Interceptors can be used to trace the publish and acknowledgments operation
    // happening in a
    //     * producer.
    //     *
    //     * @param interceptors the list of interceptors to intercept the producer created by this
    //     *     builder.
    //     * @return the producer builder instance
    //     */
    //    ProducerBuilder<T> intercept(
    //            org.apache.pulsar.client.api.interceptor.ProducerInterceptor... interceptors);

    IProducerBuilder<T> autoUpdatePartitions(boolean autoUpdate);

    IProducerBuilder<T> autoUpdatePartitionsInterval(int interval, TimeUnit unit);

    IProducerBuilder<T> enableMultiSchema(boolean multiSchema);

    IProducerBuilder<T> enableLazyStartPartitionedProducers(boolean lazyStartPartitionedProducers);
}
