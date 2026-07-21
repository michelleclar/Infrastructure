package org.carl.infra.mq.config;

import org.carl.infra.mq.consumer.SubscriptionType;
import org.carl.infra.mq.consumer.SubscriptionTypes;
import org.eclipse.microprofile.config.spi.Converter;

/** Converts the provider-neutral subscription type accepted by the common Quarkus module. */
public final class SubscriptionTypeConverter implements Converter<SubscriptionType> {

    @Override
    public SubscriptionType convert(String value) {
        if ("LOAD_BALANCED".equals(value)) {
            return SubscriptionTypes.LOAD_BALANCED;
        }
        throw new IllegalArgumentException(
                "Unsupported common MQ subscription type '"
                        + value
                        + "'. Supported value: LOAD_BALANCED");
    }
}
