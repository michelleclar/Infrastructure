package org.carl.infrastructure.workflow.persistence;

import org.carl.infrastructure.workflow.persistence.domain.snapshot.EntityStateSnapshotDto;
import org.carl.infrastructure.workflow.persistence.domain.statemiche.ActionDto;

public class WorkflowStorage {
    protected static WorkflowRepository repository;

    public static <S, E, C> void save(
            String workflowId,
            String workflowRunId,
            String entityId,
            String entityType,
            ActionDto<S, E, C> actionDto) {
        EntityStateSnapshotDto entityStateSnapshotDto = new EntityStateSnapshotDto();
        entityStateSnapshotDto.setWorkflowId(workflowId);
        entityStateSnapshotDto.setEntityId(entityId);
        entityStateSnapshotDto.setEntityType(entityType);
        entityStateSnapshotDto.setCurrentState(entityType);
        entityStateSnapshotDto.setStateData(actionDto.getContext().toString());
        entityStateSnapshotDto.setWorkflowRunId(workflowRunId);
        repository.upsertEntityStateSnapshot(entityStateSnapshotDto);
    }
}
