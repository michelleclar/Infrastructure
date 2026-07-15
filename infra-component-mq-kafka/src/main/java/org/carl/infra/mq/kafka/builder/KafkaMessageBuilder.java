package org.carl.infra.mq.kafka.builder;

import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.model.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

public class KafkaMessageBuilder<T> implements MessageBuilder<T> {
    private T value;
    private final Map<String, String> properties = new HashMap<>();
    private String key;
    private long eventTime = 0;
    private long sequenceId = -1;
    private long deliverAfter = 0;
    private long deliverAt = 0;
    private boolean disableReplication = false;
    private String messageId;
    private String topic;

    public KafkaMessageBuilder(T value) {
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
        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }

    @Override
    public MessageBuilder<T> property(String key, String value) {
        this.properties.put(key, value);
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
        return properties;
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
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
        return !properties.isEmpty();
    }

    @Override
    public boolean hasDeliverAfter() {
        return deliverAfter > 0;
    }

    @Override
    public boolean hasDeliverAt() {
        return deliverAt > 0;
    }

    @Override
    public boolean hasSequenceId() {
        return sequenceId >= 0;
    }

    @Override
    public boolean hasEventTime() {
        return eventTime > 0;
    }

    @Override
    public Message<T> build() {
        return new KafkaMessage<>(value, key, properties, eventTime, sequenceId, messageId, topic);
    }
}
