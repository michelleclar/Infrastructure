package org.carl.infrastructure.authorization;

import io.quarkus.security.identity.SecurityIdentity;

import org.carl.infrastructure.authorization.modle.UserIdentity;

/**
 * get identity {role,promise...}
 *
 * <p>check module permission
 *
 * <p>check resource permission
 */
public interface AuthProvider {

    /**
     * get identity
     *
     * @param token jwt or other
     * @return {@link IUserIdentity}
     */
    IUserIdentity getIdentity(String token);

    default IUserIdentity getIdentity() {
        return new UserIdentity(getSecurityIdentity());
    }

    SecurityIdentity getSecurityIdentity();

    /**
     * module permission
     *
     * @param userId userId
     * @param permission {@link ModulePermission}
     * @return boolean
     */
    default boolean hasModulePermission(ModulePermission permission) {
        return permission.hasPermission(getIdentity());
    }

    default boolean canAccess(ResourceIPermission permission) {
        return permission.hasPermission(getIdentity());
    }
}
