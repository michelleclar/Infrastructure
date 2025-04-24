package org.carl.infrastructure.authorization;

public class ResourcePermission implements Permission {
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
        final ResourcePermission permission;

        ResourcePermissionBuilder() {
            permission = new ResourcePermission();
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

        public ResourcePermission build() {
            // TODO check filed is null
            return permission;
        }
    }
}
