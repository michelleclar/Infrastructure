package org.carl.infrastructure.user;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.authorization.*;
import org.carl.infrastructure.authorization.modle.Permission;
import org.carl.infrastructure.authorization.modle.UserIdentity;

import java.util.Set;

/** Module authorization service (PEP) — validates module-level actions against the current user's permissions. */
@ApplicationScoped
@IfBuildProperty(name = "quarkus.plugins.user.enable", stringValue = "true")
public class UserAuthorizationService implements IModuleAuthorizationServiceAbility {
    @Inject SecurityIdentity securityIdentity;

    @Override
    public <T extends Enum<T> & IModuleEnum> boolean check(T requiredPermission) {
        Set<Permission> userPermissions =
                getIdentity().getPermissions().getOrDefault(requiredPermission.getName(), Set.of());
        for (Permission userPermission : userPermissions) {
            if (userPermission.hasPermission(requiredPermission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getModule() {
        return "user";
    }

    // Token is unused: this service delegates to the CDI-managed SecurityIdentity from the active request context.
    @Override
    public IUserIdentity getIdentity(String token) {
        return new UserIdentity(securityIdentity);
    }

    @Override
    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }
}
