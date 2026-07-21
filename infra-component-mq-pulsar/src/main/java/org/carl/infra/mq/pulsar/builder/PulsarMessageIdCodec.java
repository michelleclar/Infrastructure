package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.MessageId;

import java.io.IOException;
import java.util.Base64;

final class PulsarMessageIdCodec {

    private PulsarMessageIdCodec() {}

    static String encode(MessageId messageId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(messageId.toByteArray());
    }

    static MessageId decode(String value) throws IOException {
        try {
            return MessageId.fromByteArray(Base64.getUrlDecoder().decode(value));
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid Pulsar message id", error);
        }
    }
}
