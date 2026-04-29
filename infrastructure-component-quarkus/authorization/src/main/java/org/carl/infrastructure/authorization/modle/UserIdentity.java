package org.carl.infrastructure.authorization.modle;

import io.quarkus.security.identity.SecurityIdentity;

import org.carl.infrastructure.authorization.IUserIdentity;
import org.carl.infrastructure.authorization.modle.Permission.PermissionBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class UserIdentity implements IUserIdentity {
    Boolean isAnonymous;
    Map<String, Set<Permission>> permissions = new HashMap<>();
    Set<UserGroup> userGroups;
    Set<UserOrganize> userOrganizes;
    Set<String> roles;
    Map<String, Object> attributes;

    public UserIdentity() {}

    public UserIdentity(SecurityIdentity identity) {
        this.isAnonymous = identity.isAnonymous();
        this.permissions = new HashMap<>();
        this.userGroups = new HashSet<>();
        this.userOrganizes = new HashSet<>();
        this.roles = identity.getRoles();
        this.attributes = identity.getAttributes();
    }

    @Override
    public Boolean isAnonymous() {
        return isAnonymous;
    }

    @Override
    public Map<String, Set<Permission>> getPermissions() {
        return permissions;
    }

    @Override
    public Set<UserGroup> getGroups() {
        return userGroups;
    }

    @Override
    public Set<UserOrganize> getOrganizes() {
        return userOrganizes;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public Boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class UserIdentityBuilder {
        UserIdentity userIdentity;

        UserIdentityBuilder() {
            userIdentity = new UserIdentity();
        }

        public static UserIdentityBuilder create() {
            return new UserIdentityBuilder();
        }

        public UserIdentityBuilder setAttributes(Map<String, Object> attributes) {
            this.userIdentity.attributes = attributes;
            return this;
        }

        public UserIdentityBuilder setRoles(Set<String> roles) {
            this.userIdentity.roles = roles;
            return this;
        }

        public UserIdentityBuilder setUserOrganizes(Set<UserOrganize> userOrganizes) {
            this.userIdentity.userOrganizes = userOrganizes;
            return this;
        }

        public UserIdentityBuilder setUserGroups(Set<UserGroup> userGroups) {
            this.userIdentity.userGroups = userGroups;
            return this;
        }

        public UserIdentityBuilder setPermissions(Map<String, Set<Permission>> permissions) {
            this.userIdentity.permissions = permissions;
            return this;
        }

        public UserIdentityBuilder addPermission(String module, Permission permission) {
            Set<Permission> set = getPermission().getOrDefault(module, new HashSet<>());
            set.add(permission);
            getPermission().put(module, set);
            return this;
        }

        public UserIdentityBuilder addPermission(
                String module, Function<PermissionBuilder, Permission> permission) {
            Set<Permission> set = getPermission().getOrDefault(module, new HashSet<>());
            Permission apply = permission.apply(PermissionBuilder.create(module));
            set.add(apply);
            getPermission().put(module, set);
            return this;
        }

        public UserIdentityBuilder setAnonymous(Boolean anonymous) {
            this.userIdentity.isAnonymous = anonymous;
            return this;
        }

        private Map<String, Set<Permission>> getPermission() {
            if (userIdentity.permissions == null) {
                userIdentity.permissions = new HashMap<>();
            }
            return userIdentity.permissions;
        }

        public UserIdentity build() {
            return this.userIdentity;
        }
    }
}
