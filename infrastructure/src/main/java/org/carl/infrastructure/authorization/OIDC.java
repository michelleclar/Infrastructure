package org.carl.infrastructure.authorization;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;

public class OIDC {
    @Inject SecurityIdentity identity;
}
