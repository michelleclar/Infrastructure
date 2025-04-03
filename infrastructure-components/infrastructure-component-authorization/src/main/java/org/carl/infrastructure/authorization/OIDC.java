package org.carl.infrastructure.authorization;

import io.quarkus.security.identity.SecurityIdentity;

public interface OIDC {
    SecurityIdentity getSecurityIdentity();

    void setSecurityIdentity();
}
