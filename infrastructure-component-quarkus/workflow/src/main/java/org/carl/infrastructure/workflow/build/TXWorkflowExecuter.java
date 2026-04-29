package org.carl.infrastructure.workflow.build;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.workflow.Functions;

import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.workflow.core.ITransactionalWorkflow;
import org.jboss.logging.Logger;

public class TXWorkflowExecuter<S, E, C> {
    private final String entityId;
    private final StateMachine<S, E, C> stateMachine;
    private static final Logger LOGGER = Logger.getLogger(TXWorkflowExecuter.class);

    TXWorkflowExecuter(String entityId, StateMachine<S, E, C> stateMachine) {
        this.entityId = entityId;
        this.stateMachine = stateMachine;
    }

    public WorkflowExecution fireEvent(S from, E event, C ctx) {
        String workflowStampID =
                GeneraterWorkflowStampId.create(this.entityId, this.stateMachine.getMachineId());
        ITransactionalWorkflow workflow =
                TXWorkflowExecuterFactory.build(stateMachine, workflowStampID);

        WorkflowExecution start =
                WorkflowClient.start(
                        (Functions.Proc4<String, S, E, C>) workflow::fireEvent,
                        stateMachine.getMachineId(),
                        from,
                        event,
                        ctx);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf(
                    "TX begin by machineId:[%s], from: [%s], event: [%s], ctx:[%s]",
                    stateMachine.getMachineId(), from, event, ctx);
            LOGGER.debugf(
                    "TX workflowId:[%s], workflowRunId: [%s]",
                    start.getWorkflowId(), start.getRunId());
        }
        // FIXME
        //        if (GlobalShare.getInstance().enable().log()) {
        //            EntityStateSnapshotDto dto = new EntityStateSnapshotDto();
        //            dto.setWorkflowId(start.getWorkflowId())
        //                    .setWorkflowRunId(start.getRunId())
        //                    .setEntityId(this.entityId)
        //                    .setEntityType(this.stateMachine.getMachineId())
        //                    .setCurrentState(from.toString())
        //                    // TODO: Check this
        //                    .setStateData(ctx.toString())
        //                    .setWorkflowStamp(workflowStampID);
        //            EntityEvent entityEvent = EntityEventFactory.create(dto);
        //            try {
        //                GlobalShare.getInstance().getProducer().sendMessageAsync(entityEvent);
        //            } catch (ProducerException e) {
        //                throw new RuntimeException(e);
        //            }
        //        }

        return start;
    }
}
