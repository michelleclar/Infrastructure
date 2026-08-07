package org.carl.infra.discover.consul;

import org.carl.infra.discover.DynamicConfigValidator;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Connection, watch, and validation settings for {@link ConsulDiscoverClient}. */
public final class ConsulDiscoverOptions {

    private final URI consulUri;
    private final String aclToken;
    private final String datacenter;
    private final Duration connectTimeout;
    private final Duration blockingWait;
    private final String configKey;
    private final boolean initialConfigRequired;
    private final List<DynamicConfigValidator> validators;

    private ConsulDiscoverOptions(Builder builder) {
        consulUri = validateUri(builder.consulUri);
        aclToken = normalize(builder.aclToken);
        datacenter = normalize(builder.datacenter);
        connectTimeout = requirePositive(builder.connectTimeout, "connectTimeout");
        blockingWait = requirePositive(builder.blockingWait, "blockingWait");
        configKey = requireText(builder.configKey, "configKey");
        initialConfigRequired = builder.initialConfigRequired;
        validators = Collections.unmodifiableList(new ArrayList<>(builder.validators));
    }

    public static Builder builder(URI consulUri, String configKey) {
        return new Builder(consulUri, configKey);
    }

    public URI consulUri() {
        return consulUri;
    }

    public String aclToken() {
        return aclToken;
    }

    public String datacenter() {
        return datacenter;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration blockingWait() {
        return blockingWait;
    }

    public String configKey() {
        return configKey;
    }

    public boolean initialConfigRequired() {
        return initialConfigRequired;
    }

    public List<DynamicConfigValidator> validators() {
        return validators;
    }

    @Override
    public String toString() {
        return "ConsulDiscoverOptions{"
                + "consulUri="
                + consulUri
                + ", aclToken="
                + (aclToken.isEmpty() ? "<empty>" : "<redacted>")
                + ", datacenter='"
                + datacenter
                + '\''
                + ", connectTimeout="
                + connectTimeout
                + ", blockingWait="
                + blockingWait
                + ", configKey='"
                + configKey
                + '\''
                + ", initialConfigRequired="
                + initialConfigRequired
                + ", validators="
                + validators.size()
                + '}';
    }

    private static URI validateUri(URI value) {
        if (value == null || value.getHost() == null || value.getScheme() == null) {
            throw new IllegalArgumentException("consulUri must be an absolute HTTP(S) URI");
        }
        if (!"http".equalsIgnoreCase(value.getScheme())
                && !"https".equalsIgnoreCase(value.getScheme())) {
            throw new IllegalArgumentException("consulUri scheme must be http or https");
        }
        if (value.getUserInfo() != null) {
            throw new IllegalArgumentException("consulUri must not contain credentials");
        }
        if (value.getQuery() != null || value.getFragment() != null) {
            throw new IllegalArgumentException("consulUri must not contain a query or fragment");
        }
        if (value.getPath() != null && !value.getPath().isEmpty() && !"/".equals(value.getPath())) {
            throw new IllegalArgumentException("consulUri must not contain a path");
        }
        return value;
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    public static final class Builder {

        private final URI consulUri;
        private final String configKey;
        private String aclToken = "";
        private String datacenter = "";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration blockingWait = Duration.ofSeconds(55);
        private boolean initialConfigRequired = true;
        private final List<DynamicConfigValidator> validators = new ArrayList<>();

        private Builder(URI consulUri, String configKey) {
            this.consulUri = consulUri;
            this.configKey = configKey;
        }

        public Builder aclToken(String value) {
            aclToken = value;
            return this;
        }

        public Builder datacenter(String value) {
            datacenter = value;
            return this;
        }

        public Builder connectTimeout(Duration value) {
            connectTimeout = value;
            return this;
        }

        public Builder blockingWait(Duration value) {
            blockingWait = value;
            return this;
        }

        public Builder initialConfigRequired(boolean value) {
            initialConfigRequired = value;
            return this;
        }

        public Builder addValidator(DynamicConfigValidator validator) {
            if (validator == null) {
                throw new IllegalArgumentException("validator must not be null");
            }
            validators.add(validator);
            return this;
        }

        public ConsulDiscoverOptions build() {
            return new ConsulDiscoverOptions(this);
        }
    }
}
