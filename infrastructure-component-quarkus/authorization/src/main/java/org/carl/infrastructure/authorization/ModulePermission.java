package org.carl.infrastructure.authorization;

import jakarta.annotation.Nonnull;

import org.carl.infrastructure.authorization.modle.Permission;

import java.util.Set;

/**
 * * build by web api request ,check user have module permission from role permission
 *
 * <p>TODO: should is private?
 */
public abstract class ModulePermission implements IPermission {

    /** module name */
    String name;

    /** module description */
    String description;

    /** module action */
    private String action;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getAction() {
        return action;
    }

    @Override
    public Boolean hasPermission(IUserIdentity identity, @Nonnull String requiredAction) {
        Set<Permission> permissions = identity.getPermissions().getOrDefault(name, Set.of());
        for (Permission permission : permissions) {
            if (permission.hasPermission(requiredAction)) {
                return true;
            }
        }
        return false;
    }

    public Boolean hasPermission(IUserIdentity identity) {
        Set<Permission> permissions = identity.getPermissions().getOrDefault(name, Set.of());
        for (Permission permission : permissions) {
            if (permission.hasPermission(this.action)) {
                return true;
            }
        }
        return false;
    }

    public static class ModulePermissionBuilder {
        final ModulePermission permission;

        ModulePermissionBuilder() {
            permission = new ModulePermission() {};
        }

        public static ModulePermissionBuilder create() {
            return new ModulePermissionBuilder();
        }

        public ModulePermissionBuilder name(String name) {
            this.permission.name = name;
            return this;
        }

        public ModulePermissionBuilder description(String description) {
            this.permission.description = description;
            return this;
        }

        public ModulePermissionBuilder action(String action) {
            this.permission.action = action;
            return this;
        }

        public ModulePermission build() {
            // TODO check filed is null
            return permission;
        }
    }
}
