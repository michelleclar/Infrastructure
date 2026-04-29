package org.carl.infrastructure.authorization;

import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.authorization.modle.Permission;

import java.util.Set;

class DomainAuthorizationServiceTest {}

@ApplicationScoped
class UserAuthorizationService implements IModuleAuthorizationServiceAbility {
    final String module = "user";

    @Inject
    public UserAuthorizationService() {}

    @Override
    public <T extends Enum<T> & IModuleEnum> boolean check(T requiredPermission) {
        Set<Permission> userPermissions =
                getIdentity().getPermissions().getOrDefault(this.module, Set.of());
        for (Permission userPermission : userPermissions) {
            if (userPermission.hasPermission(requiredPermission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getModule() {
        return module;
    }

    @Override
    public IUserIdentity getIdentity(String token) {
        return null;
    }

    @Override
    public SecurityIdentity getSecurityIdentity() {
        return null;
    }
}
