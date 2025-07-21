package org.carl.infrastructure.pulsar.factory;

import java.util.HashMap;
import java.util.Map;

public class PulsarMessageBuilder<T> implements MessageBuilder<T> {
    private T value;
    private final Map<String, String> payload = new HashMap<>();
    private String key;
    private long eventTime;
    private long sequenceId;
    private long deliverAfter;
    private long deliverAt;
    private boolean disableReplication = false;
    private String messageId;
    private String topic;

    public PulsarMessageBuilder(T value) {
        this.value = value;
    }

    @Override
    public MessageBuilder<T> messageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    @Override
    public MessageBuilder<T> topic(String topic) {
        this.topic = topic;
        return this;
    }

    @Override
    public MessageBuilder<T> value(T value) {
        this.value = value;
        return this;
    }

    @Override
    public MessageBuilder<T> key(String key) {
        this.key = key;
        return this;
    }

    @Override
    public MessageBuilder<T> properties(Map<String, String> properties) {
        this.payload.putAll(properties);
        return this;
    }

    @Override
    public MessageBuilder<T> property(String key, String value) {
        this.payload.put(key, value);
        return this;
    }

    @Override
    public MessageBuilder<T> eventTime(long timestamp) {
        this.eventTime = timestamp;
        return this;
    }

    @Override
    public MessageBuilder<T> sequenceId(long sequenceId) {
        this.sequenceId = sequenceId;
        return this;
    }

    @Override
    public MessageBuilder<T> deliverAfter(long delayMillis) {
        this.deliverAfter = delayMillis;
        return this;
    }

    @Override
    public MessageBuilder<T> deliverAt(long timestamp) {
        this.deliverAt = timestamp;
        return this;
    }

    @Override
    public MessageBuilder<T> disableReplication() {
        this.disableReplication = true;
        return this;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Map<String, String> getProperties() {
        return payload;
    }

    @Override
    public String getProperty(String key) {
        return payload.get(key);
    }

    @Override
    public long getEventTime() {
        return eventTime;
    }

    @Override
    public long getSequenceId() {
        return sequenceId;
    }

    @Override
    public long getDeliverAfter() {
        return deliverAfter;
    }

    @Override
    public long getDeliverAt() {
        return deliverAt;
    }

    @Override
    public boolean isReplicationDisabled() {
        return disableReplication;
    }

    @Override
    public boolean hasKey() {
        return key != null && !key.isEmpty();
    }

    @Override
    public boolean hasProperties() {
        return !payload.isEmpty();
    }

    @Override
    public Message<T> build() {
        return new PulsarMessage<>(value, key, eventTime, sequenceId, messageId, topic);
    }

    @Override
    public boolean hasDeliverAfter() {
        return false;
    }

    @Override
    public boolean hasDeliverAt() {
        return false;
    }

    @Override
    public boolean hasSequenceId() {
        return false;
    }

    @Override
    public boolean hasEventTime() {
        return false;
    }

    public static class PulsarMessage<T> implements Message<T> {
        private final T value;
        private final Map<String, String> payload = new HashMap<>();
        private final String key;
        private final long eventTime;
        private final long sequenceId;
        private final String messageId;
        private final String topic;

        public PulsarMessage(
                T value,
                String key,
                long eventTime,
                long sequenceId,
                String messageId,
                String topic) {
            this.value = value;
            this.key = key;
            this.eventTime = eventTime;
            this.sequenceId = sequenceId;
            this.messageId = messageId;
            this.topic = topic;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public String getMessageId() {
            return messageId;
        }

        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Map<String, String> getProperties() {
            return payload;
        }

        @Override
        public String getProperty(String key) {
            return payload.get(key);
        }

        @Override
        public long getEventTime() {
            return eventTime;
        }

        @Override
        public long getSequenceId() {
            return sequenceId;
        }

        @Override
        public boolean hasKey() {
            return key != null && !key.isEmpty();
        }

        @Override
        public boolean hasProperties() {
            return !payload.isEmpty();
        }

        @Override
        public boolean hasSequenceId() {
            return sequenceId > 0;
        }

        @Override
        public boolean hasEventTime() {
            return eventTime > 0;
        }
    }
}
