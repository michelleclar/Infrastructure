package org.carl.infrastructure.audit;

import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProperty(name = "quarkus.plugins.audit.enable", stringValue = "true")
public class AuditService extends AuditStd {
}
