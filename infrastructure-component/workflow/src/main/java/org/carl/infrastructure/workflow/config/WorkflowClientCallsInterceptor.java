package org.carl.infrastructure.workflow.config;

import io.temporal.common.interceptors.WorkflowClientCallsInterceptorBase;

import org.carl.infrastructure.pulsar.common.ex.ProducerException;
import org.carl.infrastructure.workflow.event.EntityEvent;
import org.carl.infrastructure.workflow.event.EntityEventFactory;
import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateTransitionDto;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeoutException;

public class WorkflowClientCallsInterceptor extends WorkflowClientCallsInterceptorBase {
    private static final Logger LOGGER = Logger.getLogger(WorkflowClientCallsInterceptor.class);

    public WorkflowClientCallsInterceptor(
            io.temporal.common.interceptors.WorkflowClientCallsInterceptor next) {
        super(next);
    }

    //
    //    @Override
    //    public WorkflowStartOutput start(WorkflowStartInput input) {
    //        String workflowId = input.getOptions().getWorkflowId();
    //        String workflowType = input.getWorkflowType();
    //
    //        LOGGER.infof("Starting workflow: %s of type: %s", workflowId, workflowType);
    //
    //        try {
    //            long startTime = System.currentTimeMillis();
    //            WorkflowStartOutput result = super.start(input);
    //            long duration = System.currentTimeMillis() - startTime;
    //            LOGGER.infof("Workflow started successfully: {} in {}ms", workflowId, duration);
    //
    //
    //            return result;
    //        } catch (Exception e) {
    //            LOGGER.errorf("Failed to start workflow: {} of type: {}", workflowId,
    // workflowType, e);
    //            throw e;
    //        }
    //    }

    @Override
    public <R> GetResultOutput<R> getResult(GetResultInput<R> input) throws TimeoutException {
        String workflowId = input.getWorkflowExecution().getWorkflowId();

        LOGGER.debugf("Getting result for workflow: {}", workflowId);

        try {
            GetResultOutput<R> result = super.getResult(input);

            // 检查工作流是否成功完成
            R r = result.getResult();
            if (r != null) {
                LOGGER.infof("Workflow completed successfully: {}", workflowId);
                // TODO: send event message on complete
                if (GlobalShare.getInstance().enable().log()) {
                    EntityStateTransitionDto entityStateTransitionDto =
                            new EntityStateTransitionDto();
                    entityStateTransitionDto.setEntityId(workflowId);
                    entityStateTransitionDto.setWorkflowId(workflowId);
                    entityStateTransitionDto.setToState(r.toString());
                    EntityEvent entityEvent = EntityEventFactory.create(entityStateTransitionDto);
                    GlobalShare.getInstance().getProducer().sendMessageAsync(entityEvent);
                }
            } else {
                LOGGER.warnf("Workflow completed with null result: {}", workflowId);
            }

            return result;
        } catch (TimeoutException | ProducerException e) {
            LOGGER.warnf("Timeout waiting for workflow result: {}", workflowId);
            throw new RuntimeException(e);
        }
    }

    @Override
    public CancelOutput cancel(CancelInput input) {
        String workflowId = input.getWorkflowExecution().getWorkflowId();
        String reason = input.toString();

        LOGGER.infof("Cancelling workflow: {} with reason: {}", workflowId, reason);

        try {
            CancelOutput result = super.cancel(input);

            LOGGER.infof("Workflow cancelled successfully: {}", workflowId);

            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to cancel workflow: {}", workflowId, e);
            throw e;
        }
    }

    @Override
    public TerminateOutput terminate(TerminateInput input) {
        String workflowId = input.getWorkflowExecution().getWorkflowId();
        String reason = input.getReason();

        LOGGER.infof("Terminating workflow: {} with reason: {}", workflowId, reason);

        try {
            TerminateOutput result = super.terminate(input);

            LOGGER.infof("Workflow terminated successfully: {}", workflowId);

            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to terminate workflow: {}", workflowId, e);
            throw e;
        }
    }
}
