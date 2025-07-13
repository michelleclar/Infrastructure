package org.carl.infrastructure.workflow.test.publish;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import org.carl.infrastructure.workflow.test.model.ContentPublishingContext;
import org.carl.infrastructure.workflow.test.model.ContentPublishingEventEnum;
import org.carl.infrastructure.workflow.test.model.ContentPublishingStatusEnum;

@WorkflowInterface
public interface IContentPublishingWorkflow {
    @WorkflowMethod
    void publish(ContentPublishingContext context, ContentPublishingEventEnum event);

    @QueryMethod
    ContentPublishingStatusEnum queryCurrentStatus(ContentPublishingContext context);
}
