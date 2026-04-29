package org.carl.infrastructure.user;

import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.authorization.*;
import org.carl.infrastructure.authorization.modle.Permission;

import java.util.Set;

@ApplicationScoped
/**
 * 模块授权服务（PEP）
 *
 * <p>基于 {@link IUserIdentity} 的模块权限集合，校验所需动作是否允许。
 */
public class UserAuthorizationService implements IModuleAuthorizationServiceAbility {
    @Inject SecurityIdentity securityIdentity;

    /** 校验模块动作权限 */
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

    /** 模块名 */
    @Override
    public String getModule() {
        return "";
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
