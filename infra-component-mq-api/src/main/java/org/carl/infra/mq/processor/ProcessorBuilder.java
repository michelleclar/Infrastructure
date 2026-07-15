package org.carl.infra.mq.processor;

import org.carl.infra.mq.consumer.IConsumer;
import org.carl.infra.mq.producer.IProducer;
import org.carl.infra.mq.tx.ITransactional;

/** 处理器构建器 用于创建消费消息并转发到其他主题的处理器 */
public class ProcessorBuilder {
    IProducer producer;
    IConsumer consumer;
    ITransactional transactional;
}
