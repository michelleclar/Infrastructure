package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.shade.net.jcip.annotations.NotThreadSafe;
import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.consumer.ConsumerStats;
import org.carl.infrastructure.mq.consumer.IConsumer;
import org.carl.infrastructure.mq.model.Message;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 设计思路 链式调用,仿照uni api设计,每次subscribe会创建一个新consumer,
 *
 * <p>按照{@link org.apache.pulsar.client.api.ConsumerBuilder}{@link Consumer} 重新设计
 *
 * <p>ack在新的consume中 进行关闭
 *
 * @param <T>
 */
@NotThreadSafe
record PulsarConsumer<T>(Consumer<T> consumer) implements IConsumer<T>, AutoCloseable {

    @Override
    public Message<T> receive() throws ConsumerException {
        try {
            // TODO: messageListen need empty
            var receive = consumer.receive();
            return PulsarMessageBuilder.PulsarMessage.wrapper(receive);
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public Message<T> receive(int timeout, TimeUnit unit) throws ConsumerException {
        try {
            var receive = consumer.receive(timeout, unit);
            return PulsarMessageBuilder.PulsarMessage.wrapper(receive);
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public CompletableFuture<Message<T>> receiveAsync() {

        return consumer.receiveAsync().thenApply(PulsarMessageBuilder.PulsarMessage::wrapper);
    }

    @Override
    public void acknowledge(Message<T> message) throws ConsumerException {
        if (message.getSourceMessage() instanceof org.apache.pulsar.client.api.Message<?> msg) {
            try {
                consumer.acknowledge(msg);
            } catch (PulsarClientException e) {
                throw new ConsumerException(e);
            }
        } else {

            throw new ConsumerException("message type error,not instance Message");
        }
    }

    @Override
    public CompletableFuture<Void> acknowledgeAsync(
            org.carl.infrastructure.mq.model.Message<T> message) {
        if (message.getSourceMessage() instanceof org.apache.pulsar.client.api.Message<?> msg) {
            return consumer.acknowledgeAsync(msg);
        } else {
            return CompletableFuture.failedFuture(
                    new ConsumerException("message type error,not instance Message"));
        }
    }

    @Override
    public void acknowledgeCumulative(org.carl.infrastructure.mq.model.Message<T> message)
            throws ConsumerException {
        if (message.getSourceMessage() instanceof org.apache.pulsar.client.api.Message<?> msg) {
            try {
                consumer.acknowledgeCumulative(msg);
            } catch (PulsarClientException e) {
                throw new ConsumerException(e);
            }
        } else {
            throw new ConsumerException("message type error,not instance Message");
        }
    }

    @Override
    public CompletableFuture<Void> acknowledgeCumulativeAsync(
            org.carl.infrastructure.mq.model.Message<T> message) {
        if (message.getSourceMessage() instanceof org.apache.pulsar.client.api.Message<?> msg) {
            return consumer.acknowledgeCumulativeAsync(msg);
        }
        return CompletableFuture.failedFuture(
                new ConsumerException("message type error,not instance Message"));
    }

    @Override
    public void negativeAcknowledge(org.carl.infrastructure.mq.model.Message<T> message) {
        if (message.getSourceMessage() instanceof org.apache.pulsar.client.api.Message<?> msg) {
            consumer.negativeAcknowledge(msg);
        }
        throw new RuntimeException("message type error,not instance Message");
    }

    @Override
    public void pause() {
        consumer.pause();
    }

    @Override
    public void resume() {
        consumer.resume();
    }

    @Override
    public void seek(long timestamp) throws ConsumerException {
        try {
            consumer.seek(timestamp);
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public void seek(String messageId) throws ConsumerException {
        try {
            consumer.seek(MessageId.fromByteArray(messageId.getBytes()));
        } catch (IOException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public ConsumerStats getStats() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return !consumer.isConnected();
    }

    @Override
    public void close() throws ConsumerException {
        try {
            consumer.close();
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return consumer.closeAsync();
    }
}
