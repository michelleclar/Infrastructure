package org.carl.infrastructure.mq.client;

import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.consumer.IConsumerBuilder;
import org.carl.infrastructure.mq.producer.IProducerBuilder;

import java.util.concurrent.CompletableFuture;

public interface MQClient extends AutoCloseable {

    static IClientBuilder builder() {
        return null;
    }

    IProducerBuilder<byte[]> newProducer();

    <T> IProducerBuilder<T> newProducer(Class<T> clazz);

    IConsumerBuilder<byte[]> newConsumer();

    <T> IConsumerBuilder<T> newConsumer(Class<T> clazz);

    @Override
    void close() throws MQClientException;

    CompletableFuture<Void> closeAsync();

    void shutdown() throws MQClientException;

    boolean isClosed();

    //    TransactionBuilder newTransaction();

    //    /**
    //     * Create a topic reader builder with no schema ({@link Schema#BYTES}) to read from the
    // specified topic.
    //     *
    //     * <p>The Reader provides a low-level abstraction that allows for manual positioning in
    // the topic, without using a
    //     * subscription. A reader needs to be specified a {@link
    // ReaderBuilder#startMessageId(MessageId)}
    //     * that can either be:
    //     * <ul>
    //     * <li>{@link MessageId#earliest}: Start reading from the earliest message available in
    // the topic</li>
    //     * <li>{@link MessageId#latest}: Start reading from end of the topic. The first message
    // read will be the one
    //     * published <b>*after*</b> the creation of the builder</li>
    //     * <li>{@link MessageId}: Position the reader on a particular message. The first message
    // read will be the one
    //     * immediately <b>*after*</b> the specified message</li>
    //     * </ul>
    //     *
    //     * <p>A Reader can only from non-partitioned topics. In case of partitioned topics, one
    // can create the readers
    //     * directly on the individual partitions. See {@link #getPartitionsForTopic(String)} for
    // how to get the
    //     * topic partitions names.
    //     *
    //     * <p>Example of usage of Reader:
    //     * <pre>{@code
    //     * Reader<byte[]> reader = client.newReader()
    //     *        .topic("my-topic")
    //     *        .startMessageId(MessageId.earliest)
    //     *        .create();
    //     *
    //     * while (true) {
    //     *     Message<byte[]> message = reader.readNext();
    //     *     System.out.println("Got message: " + message.getValue());
    //     *     // Reader doesn't need acknowledgments
    //     * }
    //     * }</pre>
    //     *
    //     * @return a {@link ReaderBuilder} that can be used to configure and construct a {@link
    // Reader} instance
    //     * @since 2.0.0
    //     */
    //    ReaderBuilder<byte[]> newReader();

    //    /**
    //     * Create a topic reader builder with a specific {@link Schema}) to read from the
    // specified topic.
    //     *
    //     * <p>The Reader provides a low-level abstraction that allows for manual positioning in
    // the topic, without using a
    //     * subscription. A reader needs to be specified a {@link
    // ReaderBuilder#startMessageId(MessageId)} that can either
    //     * be:
    //     * <ul>
    //     * <li>{@link MessageId#earliest}: Start reading from the earliest message available in
    // the topic</li>
    //     * <li>{@link MessageId#latest}: Start reading from end of the topic. The first message
    // read will be the one
    //     * published <b>*after*</b> the creation of the builder</li>
    //     * <li>{@link MessageId}: Position the reader on a particular message. The first message
    // read will be the one
    //     * immediately <b>*after*</b> the specified message</li>
    //     * </ul>
    //     *
    //     * <p>A Reader can only from non-partitioned topics. In case of partitioned topics, one
    // can create the readers
    //     * directly on the individual partitions. See {@link #getPartitionsForTopic(String)} for
    // how to get the
    //     * topic partitions names.
    //     *
    //     * <p>Example of usage of Reader:
    //     * <pre>
    //     * {@code
    //     * Reader<String> reader = client.newReader(Schema.STRING)
    //     *        .topic("my-topic")
    //     *        .startMessageId(MessageId.earliest)
    //     *        .create();
    //     *
    //     * while (true) {
    //     *     Message<String> message = reader.readNext();
    //     *     System.out.println("Got message: " + message.getValue());
    //     *     // Reader doesn't need acknowledgments
    //     * }
    //     * }</pre>
    //     *
    //     * @return a {@link ReaderBuilder} that can be used to configure and construct a {@link
    // Reader} instance
    //     *
    //     * @since 2.0.0
    //     */
    //    <T> ReaderBuilder<T> newReader(Schema<T> schema);
    //
    //    /**
    //     * Create a table view builder for subscribing on a specific topic.
    //     *
    //     * <p>The TableView provides a key-value map view of a compacted topic. Messages without
    // keys will
    //     * be ignored.
    //     *
    //     * <p>Example:
    //     * <pre>{@code
    //     *  TableView<byte[]> tableView = client.newTableView()
    //     *            .topic("my-topic")
    //     *            .autoUpdatePartitionsInterval(5, TimeUnit.SECONDS)
    //     *            .create();
    //     *
    //     *  tableView.forEach((k, v) -> System.out.println(k + ":" + v));
    //     * }</pre>
    //     *
    //     * @return a {@link TableViewBuilder} object to configure and construct the {@link
    // TableView} instance
    //     */
    //    TableViewBuilder<byte[]> newTableView();
    //
    //    /**
    //     * Create a table view builder with a specific schema for subscribing on a specific topic.
    //     *
    //     * <p>The TableView provides a key-value map view of a compacted topic. Messages without
    // keys will
    //     * be ignored.
    //     *
    //     * <p>Example:
    //     * <pre>{@code
    //     *  TableView<byte[]> tableView = client.newTableView(Schema.BYTES)
    //     *            .topic("my-topic")
    //     *            .autoUpdatePartitionsInterval(5, TimeUnit.SECONDS)
    //     *            .create();
    //     *
    //     *  tableView.forEach((k, v) -> System.out.println(k + ":" + v));
    //     * }</pre>
    //     *
    //     * @param schema provide a way to convert between serialized data and domain objects
    //     * @return a {@link TableViewBuilder} object to configure and construct the {@link
    // TableView} instance
    //     */
    //    <T> TableViewBuilder<T> newTableView(Schema<T> schema);
    //
    //    /**
    //     * Update the service URL this client is using.
    //     *
    //     * <p>This will force the client close all existing connections and to restart service
    // discovery to the new service
    //     * endpoint.
    //     *
    //     * @param serviceUrl
    //     *            the new service URL this client should connect to
    //     * @throws PulsarClientException
    //     *             in case the serviceUrl is not valid
    //     */
    //    void updateServiceUrl(String serviceUrl) throws PulsarClientException;
    //
    //

    //    /**
    //     * 1. Get the partitions if the topic exists. Return "[{partition-0},
    // {partition-1}....{partition-n}}]" if a
    //     *   partitioned topic exists; return "[{topic}]" if a non-partitioned topic exists.
    //     * 2. When {@param metadataAutoCreationEnabled} is "false", neither the partitioned topic
    // nor non-partitioned
    //     *   topic does not exist. You will get an {@link PulsarClientException.NotFoundException}
    // or a
    //     *   {@link PulsarClientException.TopicDoesNotExistException}.
    //     *  2-1. You will get a {@link PulsarClientException.NotSupportedException} with
    // metadataAutoCreationEnabled=false
    //     *    on an old broker version which does not support getting partitions without
    // partitioned metadata auto-creation.
    //     * 3. When {@param metadataAutoCreationEnabled} is "true," it will trigger an
    // auto-creation for this topic(using
    //     *   the default topic auto-creation strategy you set for the broker), and the
    // corresponding result is returned.
    //     *   For the result, see case 1.
    //     * @version 3.3.0.
    //     */
    //    CompletableFuture<List<String>> getPartitionsForTopic(String topic, boolean
    // metadataAutoCreationEnabled);
    //

}
