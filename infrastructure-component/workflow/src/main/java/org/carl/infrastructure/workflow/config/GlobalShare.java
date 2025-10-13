package org.carl.infrastructure.workflow.config;

import org.carl.infrastructure.pulsar.builder.IConsumer;
import org.carl.infrastructure.pulsar.builder.IProducer;
import org.carl.infrastructure.workflow.event.EntityEvent;

// FIXME: remove
public class GlobalShare {

    private static final GlobalShare INSTANCE;

    private GlobalShare() {}

    static {
        INSTANCE = new GlobalShare();
    }

    public static GlobalShare getInstance() {
        return INSTANCE;
    }

    protected WorkflowArgsConfig workflowArgsConfig;
    protected IProducer<EntityEvent> producer;
    protected IConsumer<EntityEvent> consumer;

    public WorkflowArgsConfig.Enable enable() {
        return workflowArgsConfig.enable();
    }

    public IProducer<EntityEvent> getProducer() {
        return producer;
    }

    public IConsumer<EntityEvent> getConsumer() {
        return consumer;
    }
}
