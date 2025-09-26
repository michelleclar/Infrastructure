package org.carl.infrastructure.workflow.persistence.domain.snapshot;

import java.time.LocalDateTime;

public class EntityStateSnapshotDto {
    private String entityId;
    private String entityType;
    private String currentState;
    private String workflowId;
    private String workflowRunId;
    private String stateData;
    private String lastTransitionId;
    private String workflowStamp;
    private String eventType;
    private String triggerSource;
    private LocalDateTime startedAt;

    public String getEntityId() {
        return entityId;
    }

    public EntityStateSnapshotDto setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getEntityType() {
        return entityType;
    }

    public EntityStateSnapshotDto setEntityType(String entityType) {
        this.entityType = entityType;
        return this;
    }

    public String getCurrentState() {
        return currentState;
    }

    public EntityStateSnapshotDto setCurrentState(String currentState) {
        this.currentState = currentState;
        return this;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public EntityStateSnapshotDto setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public String getWorkflowRunId() {
        return workflowRunId;
    }

    public EntityStateSnapshotDto setWorkflowRunId(String workflowRunId) {
        this.workflowRunId = workflowRunId;
        return this;
    }

    public String getStateData() {
        return stateData;
    }

    public EntityStateSnapshotDto setStateData(String stateData) {
        this.stateData = stateData;
        return this;
    }

    public String getLastTransitionId() {
        return lastTransitionId;
    }

    public EntityStateSnapshotDto setLastTransitionId(String lastTransitionId) {
        this.lastTransitionId = lastTransitionId;
        return this;
    }

    public String getWorkflowStamp() {
        return workflowStamp;
    }

    public EntityStateSnapshotDto setWorkflowStamp(String workflowStamp) {
        this.workflowStamp = workflowStamp;
        return this;
    }

    public String getEventType() {
        return eventType;
    }

    public EntityStateSnapshotDto setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public EntityStateSnapshotDto setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
        return this;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public EntityStateSnapshotDto setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public EntityStateTransitionDto toEntityStateTransitionDto() {
        EntityStateTransitionDto dto = new EntityStateTransitionDto();
        dto.setEntityId(this.getEntityId());
        dto.setEntityType(this.getEntityType());
        dto.setFromState(this.getCurrentState());
        dto.setToState(this.getCurrentState());
        dto.setEventType(this.getEventType());
        dto.setWorkflowId(this.getWorkflowId());
        dto.setWorkflowRunId(this.getWorkflowRunId());
        dto.setTriggerSource(this.triggerSource);
        dto.setBusinessContext(this.stateData);
        dto.setStartedAt(this.startedAt);
        return dto;
    }
}
