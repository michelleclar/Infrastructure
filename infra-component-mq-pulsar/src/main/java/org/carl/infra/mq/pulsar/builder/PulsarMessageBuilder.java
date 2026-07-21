package org.carl.infra.mq.pulsar.builder;

import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.model.MessageBuilder;
import org.carl.infra.mq.model.MessageOption;
import org.carl.infra.mq.pulsar.producer.PulsarMessageOptions;

import java.util.HashMap;
import java.util.Map;

class PulsarMessageBuilder<T> implements MessageBuilder<T> {
    private T value;
    private final Map<String, String> payload = new HashMap<>();
    private String key;
    private long eventTime;
    private long sequenceId;
    private long deliverAfter;
    private long deliverAt;
    private boolean eventTimeSet;
    private boolean sequenceIdSet;
    private boolean deliverAfterSet;
    private boolean deliverAtSet;
    private boolean disableReplication = false;
    private String messageId;
    private String topic;

    public PulsarMessageBuilder(T value) {
        this.value = value;
    }

    @Override
    public MessageBuilder<T> option(MessageOption option) {
        PulsarMessageOptions.apply(option, this);
        return this;
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
        this.eventTimeSet = true;
        return this;
    }

    @Override
    public MessageBuilder<T> sequenceId(long sequenceId) {
        this.sequenceId = sequenceId;
        this.sequenceIdSet = true;
        return this;
    }

    @Override
    public MessageBuilder<T> deliverAfter(long delayMillis) {
        this.deliverAfter = delayMillis;
        this.deliverAfterSet = true;
        return this;
    }

    @Override
    public MessageBuilder<T> deliverAt(long timestamp) {
        this.deliverAt = timestamp;
        this.deliverAtSet = true;
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
        return new PulsarMessage<>(value, key, payload, eventTime, sequenceId, messageId, topic);
    }

    @Override
    public boolean hasDeliverAfter() {
        return deliverAfterSet;
    }

    @Override
    public boolean hasDeliverAt() {
        return deliverAtSet;
    }

    @Override
    public boolean hasSequenceId() {
        return sequenceIdSet;
    }

    @Override
    public boolean hasEventTime() {
        return eventTimeSet;
    }

    public static class PulsarMessage<T> implements Message<T> {
        private final T value;
        private final Map<String, String> payload = new HashMap<>();
        private final String key;
        private final long eventTime;
        private final long sequenceId;
        private final String messageId;
        private final String topic;
        private Object sourceMessage;

        public PulsarMessage(
                T value,
                String key,
                Map<String, String> properties,
                long eventTime,
                long sequenceId,
                String messageId,
                String topic) {
            this.value = value;
            this.key = key;
            this.payload.putAll(properties);
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

        /** TODO:设置消息源 */
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

        public static <T> Message<T> wrapper(org.apache.pulsar.client.api.Message<T> msg) {
            if (msg == null) {
                return null;
            }
            PulsarMessage<T> tPulsarMessage =
                    new PulsarMessage<>(
                            msg.getValue(),
                            msg.getKey(),
                            msg.getProperties(),
                            msg.getEventTime(),
                            msg.getSequenceId(),
                            PulsarMessageIdCodec.encode(msg.getMessageId()),
                            msg.getTopicName());
            tPulsarMessage.setSourceMessage(msg);
            return tPulsarMessage;
        }
    }
}
