package org.carl.infrastructure.audit;

import jakarta.inject.Inject;

import org.carl.infrastructure.audit.ability.IAuditAbility;
import org.carl.infrastructure.audit.core.AuditContext;

public abstract class AuditStd implements IAuditAbility {

    @Inject
    AuditContext auditContext;

    @Override
    public IAuditOperations getAudit() {
        return auditContext;
    }
}
