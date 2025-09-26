package org.carl.infrastructure.workflow.persistence.domain.snapshot;

import java.time.LocalDateTime;
import java.util.UUID;

public class EntityStateTransitionDto {
    private String entityId;
    private String entityType;
    private String workflowStamp;
    private UUID transitionId;
    private UUID transitionParentId;
    private String fromState;
    private String toState;
    private String eventType;
    private String eventData;
    private String workflowId;
    private String workflowRunId;
    private String activityId;
    private String transitionType;
    private String triggeredBy;
    private String triggerSource;
    private String businessContext;
    private String validationResult;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String status = "SUCCESS";
    private String errorCode;
    private String errorMessage;
    private Integer retryCount = 0;

    public String getEntityId() {
        return entityId;
    }

    public EntityStateTransitionDto setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getEntityType() {
        return entityType;
    }

    public EntityStateTransitionDto setEntityType(String entityType) {
        this.entityType = entityType;
        return this;
    }

    public UUID getTransitionId() {
        return transitionId;
    }

    public EntityStateTransitionDto setTransitionId(UUID transitionId) {
        this.transitionId = transitionId;
        return this;
    }

    public UUID getTransitionParentId() {
        return transitionParentId;
    }

    public EntityStateTransitionDto setTransitionParentId(UUID transitionParentId) {
        this.transitionParentId = transitionParentId;
        return this;
    }

    public String getFromState() {
        return fromState;
    }

    public EntityStateTransitionDto setFromState(String fromState) {
        this.fromState = fromState;
        return this;
    }

    public String getToState() {
        return toState;
    }

    public EntityStateTransitionDto setToState(String toState) {
        this.toState = toState;
        return this;
    }

    public String getEventType() {
        return eventType;
    }

    public EntityStateTransitionDto setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getEventData() {
        return eventData;
    }

    public EntityStateTransitionDto setEventData(String eventData) {
        this.eventData = eventData;
        return this;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public EntityStateTransitionDto setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public String getWorkflowRunId() {
        return workflowRunId;
    }

    public EntityStateTransitionDto setWorkflowRunId(String workflowRunId) {
        this.workflowRunId = workflowRunId;
        return this;
    }

    public String getActivityId() {
        return activityId;
    }

    public EntityStateTransitionDto setActivityId(String activityId) {
        this.activityId = activityId;
        return this;
    }

    public String getTransitionType() {
        return transitionType;
    }

    public EntityStateTransitionDto setTransitionType(String transitionType) {
        this.transitionType = transitionType;
        return this;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public EntityStateTransitionDto setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
        return this;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public EntityStateTransitionDto setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
        return this;
    }

    public String getBusinessContext() {
        return businessContext;
    }

    public EntityStateTransitionDto setBusinessContext(String businessContext) {
        this.businessContext = businessContext;
        return this;
    }

    public String getValidationResult() {
        return validationResult;
    }

    public EntityStateTransitionDto setValidationResult(String validationResult) {
        this.validationResult = validationResult;
        return this;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public EntityStateTransitionDto setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public EntityStateTransitionDto setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public EntityStateTransitionDto setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public EntityStateTransitionDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public EntityStateTransitionDto setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public EntityStateTransitionDto setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public EntityStateTransitionDto setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public String getWorkflowStamp() {
        return workflowStamp;
    }

    public EntityStateTransitionDto setWorkflowStamp(String workflowStamp) {
        this.workflowStamp = workflowStamp;
        return this;
    }
}
