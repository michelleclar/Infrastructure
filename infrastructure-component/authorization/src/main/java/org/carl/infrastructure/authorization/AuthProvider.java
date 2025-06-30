package org.carl.infrastructure.authorization;

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

    /**
     * module permission
     *
     * @param userId userId
     * @param permission {@link ModulePermission}
     * @return boolean
     */
    boolean hasModulePermission(String userId, ModulePermission permission);

    boolean canAccess(String userId, ResourceIPermission permission);
}
