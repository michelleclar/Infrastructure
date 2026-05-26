package org.carl.infrastructure.audit.core;

import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class AuditContextProvider {

    @Produces
    @ApplicationScoped
    @IfBuildProperty(name = "quarkus.plugins.audit.enable", stringValue = "true")
    public AuditContext get() {
        return new AuditContext();
    }
}
