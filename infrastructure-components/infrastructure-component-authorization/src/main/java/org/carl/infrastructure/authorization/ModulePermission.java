package org.carl.infrastructure.authorization;

import jakarta.annotation.Nonnull;
import java.util.Set;
import org.carl.infrastructure.authorization.modle.Permission;

/** * check user have module permission from role permission */
public abstract class ModulePermission implements IPermission {

    /** module name */
    String name;

    /** module description */
    String description;

    /** module action */
    String action;

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
