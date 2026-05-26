package org.carl.infrastructure.audit.model;

import java.time.Instant;

public class AuditEvent {
    private final long id;
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String operator;
    private final String details;
    private final Instant timestamp;

    public AuditEvent(long id, String entityType, String entityId,
                      String action, String operator, String details, Instant timestamp) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.operator = operator;
        this.details = details;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getOperator() { return operator; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}
