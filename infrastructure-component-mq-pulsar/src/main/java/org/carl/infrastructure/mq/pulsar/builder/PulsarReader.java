package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Reader;

import java.io.IOException;
import org.carl.infrastructure.mq.common.ex.ReaderException;
import org.carl.infrastructure.mq.model.Message;
import org.carl.infrastructure.mq.reader.IReader;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

record PulsarReader<T>(Reader<T> reader) implements IReader<T> {

    @Override
    public Message<T> readNext() throws ReaderException {
        try {
            return PulsarMessageBuilder.PulsarMessage.wrapper(reader.readNext());
        } catch (PulsarClientException e) {
            throw new ReaderException(e);
        }
    }

    @Override
    public Message<T> readNext(int timeout, TimeUnit unit) throws ReaderException {
        try {
            var msg = reader.readNext(timeout, unit);
            return msg == null ? null : PulsarMessageBuilder.PulsarMessage.wrapper(msg);
        } catch (PulsarClientException e) {
            throw new ReaderException(e);
        }
    }

    @Override
    public CompletableFuture<Message<T>> readNextAsync() {
        return reader.readNextAsync().thenApply(PulsarMessageBuilder.PulsarMessage::wrapper);
    }

    @Override
    public boolean hasMessageAvailable() throws ReaderException {
        try {
            return reader.hasMessageAvailable();
        } catch (PulsarClientException e) {
            throw new ReaderException(e);
        }
    }

    @Override
    public void seek(long timestamp) throws ReaderException {
        try {
            reader.seek(timestamp);
        } catch (PulsarClientException e) {
            throw new ReaderException(e);
        }
    }

    @Override
    public boolean isConnected() {
        return reader.isConnected();
    }

    @Override
    public void close() throws ReaderException {
        try {
            reader.close();
        } catch (IOException e) {
            throw new ReaderException(e);
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return reader.closeAsync();
    }
}
