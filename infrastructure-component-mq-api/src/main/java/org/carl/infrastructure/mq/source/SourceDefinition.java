package org.carl.infrastructure.mq.source;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record SourceDefinition(
        SourceReference reference,
        String className,
        String topicName,
        SourceProducerDefinition producer,
        String serdeClassName,
        String schemaType,
        Map<String, Object> configs,
        Map<String, Object> secrets,
        Integer parallelism,
        SourceProcessingGuarantees processingGuarantees,
        SourceResources resources,
        String sourceType,
        String archive,
        String runtimeFlags,
        String customRuntimeOptions,
        String batchBuilder,
        String logTopic,
        SourcePackageLocation packageLocation) {

    public SourceDefinition {
        Objects.requireNonNull(reference, "reference must not be null");
        configs = immutableCopy(configs);
        secrets = immutableCopy(secrets);
    }

    public static Builder builder(SourceReference reference) {
        return new Builder(reference);
    }

    public static Builder builder(String tenant, String namespace, String name) {
        return builder(new SourceReference(tenant, namespace, name));
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    public static final class Builder {
        private final SourceReference reference;
        private String className;
        private String topicName;
        private SourceProducerDefinition producer;
        private String serdeClassName;
        private String schemaType;
        private Map<String, Object> configs = Map.of();
        private Map<String, Object> secrets = Map.of();
        private Integer parallelism;
        private SourceProcessingGuarantees processingGuarantees;
        private SourceResources resources;
        private String sourceType;
        private String archive;
        private String runtimeFlags;
        private String customRuntimeOptions;
        private String batchBuilder;
        private String logTopic;
        private SourcePackageLocation packageLocation;

        private Builder(SourceReference reference) {
            this.reference = Objects.requireNonNull(reference, "reference must not be null");
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder topicName(String topicName) {
            this.topicName = topicName;
            return this;
        }

        public Builder producer(SourceProducerDefinition producer) {
            this.producer = producer;
            return this;
        }

        public Builder serdeClassName(String serdeClassName) {
            this.serdeClassName = serdeClassName;
            return this;
        }

        public Builder schemaType(String schemaType) {
            this.schemaType = schemaType;
            return this;
        }

        public Builder configs(Map<String, Object> configs) {
            this.configs = configs;
            return this;
        }

        public Builder secrets(Map<String, Object> secrets) {
            this.secrets = secrets;
            return this;
        }

        public Builder parallelism(Integer parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public Builder processingGuarantees(SourceProcessingGuarantees processingGuarantees) {
            this.processingGuarantees = processingGuarantees;
            return this;
        }

        public Builder resources(SourceResources resources) {
            this.resources = resources;
            return this;
        }

        public Builder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder archive(String archive) {
            this.archive = archive;
            return this;
        }

        public Builder runtimeFlags(String runtimeFlags) {
            this.runtimeFlags = runtimeFlags;
            return this;
        }

        public Builder customRuntimeOptions(String customRuntimeOptions) {
            this.customRuntimeOptions = customRuntimeOptions;
            return this;
        }

        public Builder batchBuilder(String batchBuilder) {
            this.batchBuilder = batchBuilder;
            return this;
        }

        public Builder logTopic(String logTopic) {
            this.logTopic = logTopic;
            return this;
        }

        public Builder packageLocation(SourcePackageLocation packageLocation) {
            this.packageLocation = packageLocation;
            return this;
        }

        public Builder packageFile(String fileName) {
            this.packageLocation = SourcePackageLocation.file(fileName);
            return this;
        }

        public Builder packageUrl(String url) {
            this.packageLocation = SourcePackageLocation.url(url);
            return this;
        }

        public SourceDefinition build() {
            return new SourceDefinition(
                    reference,
                    className,
                    topicName,
                    producer,
                    serdeClassName,
                    schemaType,
                    configs,
                    secrets,
                    parallelism,
                    processingGuarantees,
                    resources,
                    sourceType,
                    archive,
                    runtimeFlags,
                    customRuntimeOptions,
                    batchBuilder,
                    logTopic,
                    packageLocation);
        }
    }
}
