package org.carl.infrastructure.mq.pulsar.source;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.common.functions.FunctionConfig;
import org.apache.pulsar.common.functions.ProducerConfig;
import org.apache.pulsar.common.functions.Resources;
import org.apache.pulsar.common.io.SourceConfig;
import org.carl.infrastructure.mq.source.SourceDefinition;
import org.carl.infrastructure.mq.source.SourceProducerDefinition;
import org.carl.infrastructure.mq.source.SourceResources;

final class PulsarSourceConfigMapper {

    private PulsarSourceConfigMapper() {}

    static SourceConfig toPulsar(SourceDefinition source) {
        SourceConfig config = new SourceConfig();
        config.setTenant(source.reference().tenant());
        config.setNamespace(source.reference().namespace());
        config.setName(source.reference().name());
        config.setClassName(source.className());
        config.setTopicName(source.topicName());
        config.setProducerConfig(toPulsar(source.producer()));
        config.setSerdeClassName(source.serdeClassName());
        config.setSchemaType(source.schemaType());
        config.setConfigs(source.configs());
        config.setSecrets(source.secrets());
        config.setParallelism(source.parallelism());
        config.setProcessingGuarantees(
                source.processingGuarantees() == null
                        ? null
                        : FunctionConfig.ProcessingGuarantees.valueOf(
                                source.processingGuarantees().name()));
        config.setResources(toPulsar(source.resources()));
        config.setSourceType(source.sourceType());
        config.setArchive(source.archive());
        config.setRuntimeFlags(source.runtimeFlags());
        config.setCustomRuntimeOptions(source.customRuntimeOptions());
        config.setBatchBuilder(source.batchBuilder());
        config.setLogTopic(source.logTopic());
        return config;
    }

    private static ProducerConfig toPulsar(SourceProducerDefinition producer) {
        if (producer == null) {
            return null;
        }
        ProducerConfig config = new ProducerConfig();
        config.setMaxPendingMessages(producer.maxPendingMessages());
        config.setMaxPendingMessagesAcrossPartitions(producer.maxPendingMessagesAcrossPartitions());
        config.setUseThreadLocalProducers(producer.useThreadLocalProducers());
        config.setBatchBuilder(producer.batchBuilder());
        config.setCompressionType(
                producer.compressionType() == null
                        ? null
                        : CompressionType.valueOf(producer.compressionType()));
        return config;
    }

    private static Resources toPulsar(SourceResources resources) {
        if (resources == null) {
            return null;
        }
        return new Resources(resources.cpu(), resources.ram(), resources.disk());
    }
}
