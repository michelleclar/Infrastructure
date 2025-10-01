package org.carl.infrastructure.pulsar.builder;

import net.jcip.annotations.NotThreadSafe;

import org.apache.pulsar.client.api.*;
import org.carl.infrastructure.pulsar.common.ex.ConsumerException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 设计思路 链式调用,仿照uni api设计,每次subscribe会创建一个新consumer,
 *
 * <p>按照{@link ConsumerBuilder}{@link Consumer} 重新设计
 *
 * <p>ack在新的consume中 进行关闭
 *
 * @param <T>
 */
@NotThreadSafe
public record PulsarConsumer<T>(Consumer<T> consumer) implements IConsumer<T>, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(PulsarConsumer.class);

    @Override
    public MessageBuilder.Message<T> receive() throws ConsumerException {
        try {
            // TODO: messageListen need empty
            Message<T> receive = consumer.receive();
            return PulsarMessageBuilder.PulsarMessage.wrapper(receive);
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public MessageBuilder.Message<T> receive(int timeout, TimeUnit unit) throws ConsumerException {
        try {
            Message<T> receive = consumer.receive(timeout, unit);
            return PulsarMessageBuilder.PulsarMessage.wrapper(receive);
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public CompletableFuture<MessageBuilder.Message<T>> receiveAsync() {

        return consumer.receiveAsync().thenApply(PulsarMessageBuilder.PulsarMessage::wrapper);
    }

    @Override
    public void acknowledge(MessageBuilder.Message<T> message) throws ConsumerException {
        if (message.getSourceMessage() instanceof Message<?> msg) {
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
    public CompletableFuture<Void> acknowledgeAsync(MessageBuilder.Message<T> message) {
        if (message.getSourceMessage() instanceof Message<?> msg) {
            return consumer.acknowledgeAsync(msg);
        } else {
            return CompletableFuture.failedFuture(
                    new ConsumerException("message type error,not instance Message"));
        }
    }

    @Override
    public void acknowledgeCumulative(MessageBuilder.Message<T> message) throws ConsumerException {
        if (message.getSourceMessage() instanceof Message<?> msg) {
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
    public CompletableFuture<Void> acknowledgeCumulativeAsync(MessageBuilder.Message<T> message) {
        if (message.getSourceMessage() instanceof Message<?> msg) {
            return consumer.acknowledgeCumulativeAsync(msg);
        }
        return CompletableFuture.failedFuture(
                new ConsumerException("message type error,not instance Message"));
    }

    @Override
    public void negativeAcknowledge(MessageBuilder.Message<T> message) {
        if (message.getSourceMessage() instanceof Message<?> msg) {
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
