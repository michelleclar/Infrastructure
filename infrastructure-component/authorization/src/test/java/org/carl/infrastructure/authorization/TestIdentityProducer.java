package org.carl.infrastructure.authorization;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.carl.infrastructure.authorization.modle.UserIdentity;

import java.util.HashMap;
import java.util.HashSet;

@ApplicationScoped
public class TestIdentityProducer {
    @Produces
    public IUserIdentity userIdentity() {
        return UserIdentity.UserIdentityBuilder.create()
                .setAnonymous(false)
                .setRoles(new HashSet<>())
                .setAttributes(new HashMap<>())
                .setPermissions(new HashMap<>())
                .build();
    }
}
