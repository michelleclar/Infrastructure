package org.carl.infrastructure.authorization;

import jakarta.annotation.Nonnull;

public abstract class ResourceIPermission implements IPermission {
    String name;
    String resourceId;
    String resourceType;
    String description;
    String relation;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Boolean hasPermission(IUserIdentity identity, @Nonnull String requiredAction) {
        return null;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getRelation() {
        return relation;
    }

    public static class ResourcePermissionBuilder {
        final ResourceIPermission permission;

        ResourcePermissionBuilder() {
            permission = new ResourceIPermission() {};
        }

        public static ResourcePermissionBuilder create() {
            return new ResourcePermissionBuilder();
        }

        public ResourcePermissionBuilder name(String name) {
            this.permission.name = name;
            return this;
        }

        public ResourcePermissionBuilder description(String description) {
            this.permission.description = description;
            return this;
        }

        public ResourcePermissionBuilder resourceId(String resourceId) {
            this.permission.resourceId = resourceId;
            return this;
        }

        public ResourcePermissionBuilder resourceType(String resourceType) {
            this.permission.resourceType = resourceType;
            return this;
        }

        public ResourcePermissionBuilder relation(String relation) {
            this.permission.relation = relation;
            return this;
        }

        public ResourceIPermission build() {
            // TODO check filed is null
            return permission;
        }
    }
}
