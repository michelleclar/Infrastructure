package org.carl.infrastructure.authorization.modle;

import org.carl.infrastructure.authorization.IUserIdentity;
import org.carl.infrastructure.util.Conversion;

import java.util.Map;
import java.util.Set;

public class UserIdentity implements IUserIdentity {
    Boolean isAnonymous;
    Map<String, Set<Permission>> permissions;
    Set<UserGroup> userGroups;
    Set<UserOrganize> userOrganizes;
    Set<String> roles;
    Map<String, Object> attributes;

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
    public Conversion getAttribute(String name) {
        return Conversion.create(attributes.get(name));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public UserIdentity setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public UserIdentity setRoles(Set<String> roles) {
        this.roles = roles;
        return this;
    }

    public UserIdentity setUserOrganizes(Set<UserOrganize> userOrganizes) {
        this.userOrganizes = userOrganizes;
        return this;
    }

    public UserIdentity setUserGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups;
        return this;
    }

    public UserIdentity setPermissions(Map<String, Set<Permission>> permissions) {
        this.permissions = permissions;
        return this;
    }

    public UserIdentity setAnonymous(Boolean anonymous) {
        isAnonymous = anonymous;
        return this;
    }
}
