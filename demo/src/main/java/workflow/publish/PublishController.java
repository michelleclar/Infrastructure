package workflow.publish;

import io.smallrye.mutiny.Uni;
import io.temporal.api.common.v1.WorkflowExecution;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.carl.infrastructure.component.web.annotations.ControllerLogged;
import org.carl.infrastructure.workflow.build.TXWorkflowBuilder;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;

import workflow.model.PublishRequest;

@Path("/publish")
@ControllerLogged
public class PublishController {

    @Inject Logger logger;

    @POST
    public Uni<String> publish(@RequestBody PublishRequest request) {
        WorkflowExecution we =
                TXWorkflowBuilder.of(ContentPublishingStateMachine.STATEMACHINE)
                        .txStamp(
                                ContentPublishingStateMachine.STATEMACHINE.getMachineId()
                                        + request.getContext().getEntityId())
                        .fireEvent(request.getStatus(), request.getEvent(), request.getContext());

        logger.infof("Publish workflow started: [%s],[%s]", we.getRunId(), we.getWorkflowId());
        return Uni.createFrom().item(we.getRunId());
    }
}
