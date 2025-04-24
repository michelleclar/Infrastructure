package org.carl.infrastructure.authorization;

public interface ITenantIdAbility {
    default String getTenantId() {
        // TODO: tenantId userId to tenantId
        // TODO: header ger tenantId
        return System.getenv("TENANT_ID");
    }
}
