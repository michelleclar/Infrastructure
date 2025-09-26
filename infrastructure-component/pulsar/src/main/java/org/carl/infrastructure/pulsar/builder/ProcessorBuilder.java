package org.carl.infrastructure.pulsar.builder;

import org.carl.infrastructure.pulsar.core.IConsumer;
import org.carl.infrastructure.pulsar.core.IProducer;
import org.carl.infrastructure.pulsar.core.ITransactional;

/** 处理器构建器 用于创建消费消息并转发到其他主题的处理器 */
public class ProcessorBuilder {
    IProducer producer;
    IConsumer consumer;
    ITransactional transactional;
}
