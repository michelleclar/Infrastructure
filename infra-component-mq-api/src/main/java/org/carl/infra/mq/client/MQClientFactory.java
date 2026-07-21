package org.carl.infra.mq.client;

import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/** Public entry point for creating an MQ client without importing a concrete implementation. */
public final class MQClientFactory {

    private MQClientFactory() {}

    /**
     * Creates a client with the single provider present on the runtime classpath.
     *
     * @throws MQClientException when no provider or more than one provider is present
     */
    public static MQClient create(MQConfig config) throws MQClientException {
        Objects.requireNonNull(config, "config");
        try {
            return create(config, ServiceLoader.load(MQClientProvider.class));
        } catch (ServiceConfigurationError error) {
            throw new MQClientException("Failed to load MQ client provider", error);
        }
    }

    static MQClient create(MQConfig config, Iterable<MQClientProvider> discoveredProviders)
            throws MQClientException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(discoveredProviders, "discoveredProviders");

        List<MQClientProvider> providers = new ArrayList<>();
        discoveredProviders.forEach(providers::add);

        if (providers.isEmpty()) {
            throw new MQClientException(
                    "No MQ client provider found. Add exactly one MQ provider implementation to the runtime classpath");
        }
        if (providers.size() > 1) {
            String providerNames =
                    providers.stream()
                            .map(MQClientProvider::name)
                            .sorted(Comparator.naturalOrder())
                            .reduce((left, right) -> left + ", " + right)
                            .orElse("");
            throw new MQClientException(
                    "Multiple MQ client providers found: "
                            + providerNames
                            + ". Keep exactly one provider implementation on the runtime classpath");
        }

        MQClientProvider provider = providers.getFirst();
        MQClient client = provider.create(config);
        if (client == null) {
            throw new MQClientException(
                    "MQ client provider '" + provider.name() + "' returned a null client");
        }
        return client;
    }
}
