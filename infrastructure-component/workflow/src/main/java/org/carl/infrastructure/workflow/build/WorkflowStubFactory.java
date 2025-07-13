package org.carl.infrastructure.workflow.build;

import io.temporal.client.WorkflowClient;

import org.carl.infrastructure.workflow.config.WorkerManger;

public class WorkflowStubFactory {
    private static final WorkflowClient workflowClient = WorkerManger.getWorkflowClient();
}
