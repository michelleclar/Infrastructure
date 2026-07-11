package org.carl.infrastructure.mq.kafka.builder;

import org.carl.infrastructure.mq.model.Message;

import java.util.HashMap;
import java.util.Map;

public class KafkaMessage<T> implements Message<T> {
    private final T value;
    private final Map<String, String> properties;
    private final String key;
    private final long eventTime;
    private final long sequenceId;
    private final String messageId;
    private final String topic;
    private Object sourceMessage;

    public KafkaMessage(
            T value,
            String key,
            Map<String, String> properties,
            long eventTime,
            long sequenceId,
            String messageId,
            String topic) {
        this.value = value;
        this.key = key;
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
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
    public Object getSourceMessage() {
        return sourceMessage;
    }

    public void setSourceMessage(Object sourceMessage) {
        this.sourceMessage = sourceMessage;
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
    public boolean hasKey() {
        return key != null && !key.isEmpty();
    }

    @Override
    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    @Override
    public boolean hasSequenceId() {
        return sequenceId >= 0;
    }

    @Override
    public boolean hasEventTime() {
        return eventTime > 0;
    }
}
