package org.carl.infra.workflow.runtime;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import org.carl.infra.workflow.spi.WorkflowEvent;

/**
 * Single generic Temporal workflow that interprets any {@link
 * org.carl.infra.workflow.definition.WorkflowDefinition}.
 *
 * <p>The workflow body is implemented by {@link GenericWorkflowImpl}. Business code never writes a
 * {@code @WorkflowInterface}: the entire flow is driven by the {@link
 * WorkflowInput#workflowDefinition()} + handler registry pair.
 */
@WorkflowInterface
public interface GenericWorkflow {

    @WorkflowMethod
    WorkflowResult execute(WorkflowInput input);

    @SignalMethod
    void signal(WorkflowEvent event);

    @QueryMethod
    WorkflowState query();
}
