package org.carl.infrastructure.audit.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class AuditContextProvider {

    @Produces
    @ApplicationScoped
    public AuditContext get() {
        return new AuditContext();
    }
}
