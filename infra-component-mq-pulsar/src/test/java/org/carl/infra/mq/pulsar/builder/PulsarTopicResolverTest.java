package org.carl.infra.mq.pulsar.builder;

import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PulsarTopicResolverTest {

    @Test
    void resolvesShortTopicsUsingConfiguredNamespace() {
        PulsarConfig config =
                new PulsarConfig("pulsar://localhost:6650")
                        .topicTenant("public")
                        .topicNamespace("test");

        PulsarTopicResolver resolver = PulsarTopicResolver.from(config);

        assertEquals("persistent://public/test/orders", resolver.resolve("orders"));
    }

    @Test
    void supportsTheThreeRequiredEnvironmentNamespaces() {
        assertEquals(
                "persistent://public/test/orders",
                new PulsarTopicResolver("public", "test").resolve("orders"));
        assertEquals(
                "persistent://public/pro/orders",
                new PulsarTopicResolver("public", "pro").resolve("orders"));
        assertEquals(
                "persistent://public/prod/orders",
                new PulsarTopicResolver("public", "prod").resolve("orders"));
    }

    @Test
    void keepsFullyQualifiedTopicsUnchanged() {
        PulsarTopicResolver resolver = new PulsarTopicResolver("public", "test");

        assertEquals(
                "persistent://other/namespace/orders",
                resolver.resolve("persistent://other/namespace/orders"));
        assertEquals(
                "non-persistent://other/namespace/orders",
                resolver.resolve("non-persistent://other/namespace/orders"));
    }

    @Test
    void resolvesPatternsAndPreservesFlags() {
        PulsarTopicResolver resolver = new PulsarTopicResolver("public", "prod");
        Pattern resolved = resolver.resolve(Pattern.compile("orders-.*", Pattern.CASE_INSENSITIVE));

        assertEquals("persistent://public/prod/orders-.*", resolved.pattern());
        assertEquals(Pattern.CASE_INSENSITIVE, resolved.flags());
    }

    @Test
    void rejectsUnsupportedTopicSchemes() {
        PulsarTopicResolver resolver = new PulsarTopicResolver("public", "test");

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("http://orders"));
    }
}
