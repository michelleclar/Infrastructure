package org.carl.infrastructure.audit.ability;

import org.carl.infrastructure.audit.IAuditOperations;
import org.carl.infrastructure.audit.model.AuditEvent;

import java.util.List;

public interface IAuditAbility {
    IAuditOperations getAudit();

    default void auditRecord(AuditEvent event) {
        getAudit().record(event);
    }

    default void auditRecord(String entityType, String entityId, String action, String details) {
        getAudit().record(entityType, entityId, action, details);
    }

    default List<AuditEvent> auditQuery(String entityType, String entityId) {
        return getAudit().query(entityType, entityId);
    }
}
