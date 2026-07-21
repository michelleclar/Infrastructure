package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.MessageId;
import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.pulsar.producer.PulsarMessageOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

class PulsarMessageBuilderTest {

    @Test
    void preservesConfiguredMessageMetadata() {
        PulsarMessageBuilder<String> builder = new PulsarMessageBuilder<>("value");
        builder.key("key")
                .property("trace-id", "trace-1")
                .eventTime(123L)
                .sequenceId(0L)
                .deliverAfter(500L)
                .deliverAt(1_000L);

        assertTrue(builder.hasEventTime());
        assertTrue(builder.hasSequenceId());
        assertTrue(builder.hasDeliverAfter());
        assertTrue(builder.hasDeliverAt());

        Message<String> message = builder.build();
        assertEquals("trace-1", message.getProperty("trace-id"));
        assertEquals(123L, message.getEventTime());
        assertEquals(0L, message.getSequenceId());
    }

    @Test
    void messageIdEncodingCanBeUsedForSeek() throws Exception {
        String encoded = PulsarMessageIdCodec.encode(MessageId.earliest);

        assertEquals(MessageId.earliest, PulsarMessageIdCodec.decode(encoded));
    }

    @Test
    void appliesPulsarMessageOptionThroughTheCommonMethod() {
        PulsarMessageBuilder<String> builder = new PulsarMessageBuilder<>("value");

        builder.option(PulsarMessageOptions.deliverAfter(Duration.ofMillis(250)));

        assertTrue(builder.hasDeliverAfter());
        assertEquals(250L, builder.getDeliverAfter());
    }
}
