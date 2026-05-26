package org.carl.infrastructure.audit.model;

import java.time.Instant;
import java.util.Objects;

public class AuditEvent {
    private long id;
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String operator;
    private final String details;
    private final Instant timestamp;

    public AuditEvent(long id, String entityType, String entityId,
                      String action, String operator, String details, Instant timestamp) {
        this.entityType = Objects.requireNonNull(entityType, "entityType must not be null");
        this.entityId = Objects.requireNonNull(entityId, "entityId must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.id = id;
        this.details = details;
        this.timestamp = timestamp;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getOperator() { return operator; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}
