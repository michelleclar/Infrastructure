package org.carl.infrastructure.authorization;

import jakarta.enterprise.context.ApplicationScoped;

import org.carl.infrastructure.authorization.modle.Permission;

import java.util.Set;

class DomainAuthorizationServiceTest {}
@ApplicationScoped
class UserAuthorizationService implements IModuleAuthorizationServiceAbility {
    private final IUserIdentity identity;
    final String module = "user";

    public UserAuthorizationService(IUserIdentity identity) {
        this.identity = identity;
    }

    @Override
    public <T extends Enum<T> & IModuleEnum> boolean check(T requiredPermission) {
        Set<Permission> userPermissions =
                identity.getPermissions().getOrDefault(this.module, Set.of());
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
}
