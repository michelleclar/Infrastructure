package org.carl.infrastructure.mq.source;

public record SourceProducerDefinition(
        Integer maxPendingMessages,
        Integer maxPendingMessagesAcrossPartitions,
        Boolean useThreadLocalProducers,
        String batchBuilder,
        String compressionType) {}
