package org.carl.infrastructure.audit;

import org.carl.infrastructure.audit.model.AuditEvent;

import java.util.List;

public interface IAuditOperations {
    void record(AuditEvent event);
    void record(String entityType, String entityId, String action, String details);
    List<AuditEvent> query(String entityType, String entityId);
}
