package org.carl.infra.mq.client;

import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;

/**
 * Creates an {@link MQClient} for one concrete messaging implementation.
 *
 * <p>Provider implementations are discovered through {@link java.util.ServiceLoader}. An
 * application must place exactly one provider implementation on its runtime classpath.
 */
public interface MQClientProvider {

    /** Stable provider name used in diagnostics. */
    String name();

    /** Creates a client from the common MQ configuration contract. */
    MQClient create(MQConfig config) throws MQClientException;
}
