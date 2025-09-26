package org.carl.infrastructure.workflow.persistence.domain.snapshot;

import org.jooq.JSONB;

import java.time.LocalDateTime;

public class EntityCompensationDto {
    private String originalTransitionId;
    private String entityId;
    private String entityType;
    private String compensationType;
    private String compensationAction;
    private JSONB compensationData;
    private String workflowId;
    private String executedBy;
    private String executionReason;
    private String status;
    private JSONB resultData;
    private String errorMessage;
    private LocalDateTime completedAt;

    // getters and setters
    public String getOriginalTransitionId() {
        return originalTransitionId;
    }

    public void setOriginalTransitionId(String originalTransitionId) {
        this.originalTransitionId = originalTransitionId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getCompensationType() {
        return compensationType;
    }

    public void setCompensationType(String compensationType) {
        this.compensationType = compensationType;
    }

    public String getCompensationAction() {
        return compensationAction;
    }

    public void setCompensationAction(String compensationAction) {
        this.compensationAction = compensationAction;
    }

    public JSONB getCompensationData() {
        return compensationData;
    }

    public void setCompensationData(JSONB compensationData) {
        this.compensationData = compensationData;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public String getExecutionReason() {
        return executionReason;
    }

    public void setExecutionReason(String executionReason) {
        this.executionReason = executionReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JSONB getResultData() {
        return resultData;
    }

    public void setResultData(JSONB resultData) {
        this.resultData = resultData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
