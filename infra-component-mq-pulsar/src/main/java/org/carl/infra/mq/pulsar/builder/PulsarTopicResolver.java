package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.common.naming.TopicName;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.pulsar.config.PulsarConfig;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Resolves short topic names against one configured Pulsar tenant and namespace. */
final class PulsarTopicResolver {

    private static final String PERSISTENT_SCHEME = "persistent://";
    private static final String NON_PERSISTENT_SCHEME = "non-persistent://";
    private static final String DEFAULT_TENANT = "public";
    private static final String DEFAULT_NAMESPACE = "default";

    private final String topicPrefix;

    PulsarTopicResolver(String tenant, String namespace) {
        String checkedTenant = requireText(tenant, "topicTenant");
        String checkedNamespace = requireText(namespace, "topicNamespace");
        this.topicPrefix = PERSISTENT_SCHEME + checkedTenant + "/" + checkedNamespace + "/";
        TopicName.get(topicPrefix + "namespace-validation");
    }

    static PulsarTopicResolver from(MQConfig config) {
        if (config instanceof PulsarConfig pulsarConfig) {
            return new PulsarTopicResolver(
                    pulsarConfig.getTopicTenant(), pulsarConfig.getTopicNamespace());
        }
        return defaults();
    }

    static PulsarTopicResolver defaults() {
        return new PulsarTopicResolver(DEFAULT_TENANT, DEFAULT_NAMESPACE);
    }

    String resolve(String topic) {
        String checkedTopic = requireText(topic, "topic");
        if (isFullyQualified(checkedTopic)) {
            return TopicName.get(checkedTopic).toString();
        }
        rejectUnsupportedScheme(checkedTopic);
        return TopicName.get(topicPrefix + checkedTopic).toString();
    }

    List<String> resolve(List<String> topics) {
        Objects.requireNonNull(topics, "topics");
        return topics.stream().map(this::resolve).toList();
    }

    String[] resolve(String... topics) {
        Objects.requireNonNull(topics, "topics");
        String[] resolved = new String[topics.length];
        for (int index = 0; index < topics.length; index++) {
            resolved[index] = resolve(topics[index]);
        }
        return resolved;
    }

    Pattern resolve(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return Pattern.compile(resolvePatternText(pattern.pattern()), pattern.flags());
    }

    String resolvePatternText(String pattern) {
        String checkedPattern = requireText(pattern, "topicsPattern");
        if (isFullyQualified(checkedPattern)) {
            return checkedPattern;
        }
        rejectUnsupportedScheme(checkedPattern);
        return topicPrefix + checkedPattern;
    }

    private static boolean isFullyQualified(String value) {
        return value.startsWith(PERSISTENT_SCHEME) || value.startsWith(NON_PERSISTENT_SCHEME);
    }

    private static void rejectUnsupportedScheme(String value) {
        if (value.contains("://")) {
            throw new IllegalArgumentException(
                    "Unsupported Pulsar topic scheme. Use persistent:// or non-persistent://: "
                            + value);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
