package org.carl.infrastructure.authorization;

import java.util.Map;
import java.util.Set;
import org.carl.infrastructure.authorization.modle.Permission;
import org.carl.infrastructure.authorization.modle.UserGroup;
import org.carl.infrastructure.authorization.modle.UserOrganize;

/**
 * roles: main
 *
 * <p>domain permissions: page,action
 *
 * <p>resource policies: resource ,conditions ,line controller domain and resource controller by
 * roles roles -> domain roles -> resource
 *
 * <pre>
 *
 *
 * user identity =
 * {
 * "group": [
 * "a_team"
 * ],
 * "org": [
 * {
 * "name": "x_org",
 * "group": "x_team"
 * }
 * ],
 * "roles": [
 * {
 * "name": "manager",
 * "inherits": [
 * "admin"
 * ]
 * }
 * ],
 * "permissions": [
 * {
 * "module": "user. order",
 * "actions": [
 * {
 * "name": "view",
 * "enable": "true"
 * },
 * {
 * "name": "edit",
 * "enable": "false"
 * }
 * ]
 * },
 * {
 * "module": "document. article",
 * "actions": [
 * {
 * "name": "view",
 * "enable": "true"
 * },
 * {
 * "name": "edit",
 * "enable": "false"
 * }
 * ]
 * }
 * ]
 * }
 *
 * </pre>
 */
public interface IUserIdentity {

    String USER_ATTRIBUTE = "auth.user.identity";

    Boolean isAnonymous();

    Map<String, Set<Permission>> getPermissions();

    Set<UserGroup> getGroups();

    Set<UserOrganize> getOrganizes();

    Set<String> getRoles();

    Boolean hasRole(String role);

    Object getAttribute(String name);

    Map<String, Object> getAttributes();
}
