package org.carl.infrastructure.workflow.config;

import io.quarkus.runtime.StartupEvent;
import io.temporal.client.WorkflowClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.pulsar.client.api.PulsarClient;
import org.carl.infrastructure.pulsar.common.ex.ConsumerException;
import org.carl.infrastructure.pulsar.common.ex.ProducerException;
import org.carl.infrastructure.pulsar.config.MsgArgsConfig;
import org.carl.infrastructure.workflow.persistence.WorkflowRepository;
import org.jboss.logging.Logger;

/**
 * not work
 *
 * <p>need disable quarkus.temporal.workflow.disable-eager-execution=false
 */
@ApplicationScoped
public class WorkflowLifecycle {
    private static final Logger LOGGER = Logger.getLogger(WorkflowLifecycle.class);
    @Inject WorkflowArgsConfig workflowArgsConfig;
    @Inject PulsarClient pulsarClient;
    @Inject WorkflowRepository workflowRepository;
    @Inject MsgArgsConfig msgArgsConfig;
    @Inject WorkflowClient client;

    void onStart(@Observes StartupEvent ev) throws ProducerException, ConsumerException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("workflow started args :[%s]", workflowArgsConfig);
        }
        GlobalShare.getInstance().workflowArgsConfig = workflowArgsConfig;
        if (GlobalShare.getInstance().workflowArgsConfig.enable().log()) {
            String topic = "workflow-event";
            //            GlobalShare.getInstance().consumer =
            //                    PulsarFactory.createConsumer(
            //                            pulsarClient, msgArgsConfig.consumer(), EntityEvent.class,
            // topic);
            //            GlobalShare.getInstance().producer =
            //                    PulsarFactory.createProducer(
            //                            pulsarClient, msgArgsConfig.producer(), EntityEvent.class,
            // topic);
            //            GlobalShare.getInstance()
            //                    .consumer
            //                    .subscribeName("sub-workflow-event")
            //                    .setMessageListener(
            //                            (consumer, message) -> {
            //                                switch (message.getValue().getEventTypeEnum()) {
            //                                    case snapshot -> {
            //                                        workflowRepository.upsertEntityStateSnapshot(
            //
            // message.getValue().getEntityStateSnapshotDto());
            //                                    }
            //                                    case transfer -> {
            //                                        workflowRepository.updateCompensationStatus(
            //
            // message.getValue().getEntityCompensationDto());
            //                                    }
            //                                }
            //                            })
            //                    .subscribe();
        }
        WorkerManger.workflowClient = client;
    }
}
