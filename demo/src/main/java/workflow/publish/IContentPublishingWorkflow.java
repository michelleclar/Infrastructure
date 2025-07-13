package workflow.publish;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import workflow.model.ContentPublishingContext;
import workflow.model.ContentPublishingEventEnum;
import workflow.model.ContentPublishingStatusEnum;

@WorkflowInterface
public interface IContentPublishingWorkflow {
    @WorkflowMethod
    void publish(ContentPublishingContext context, ContentPublishingEventEnum event);

    @QueryMethod
    ContentPublishingStatusEnum queryCurrentStatus(ContentPublishingContext context);
}
