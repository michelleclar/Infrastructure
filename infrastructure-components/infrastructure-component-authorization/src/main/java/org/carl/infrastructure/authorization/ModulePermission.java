package org.carl.infrastructure.authorization;

public class ModulePermission implements Permission {
    String name;
    String description;
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

    public static class ModulePermissionBuilder {
        final ModulePermission permission;

        ModulePermissionBuilder() {
            permission = new ModulePermission();
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
