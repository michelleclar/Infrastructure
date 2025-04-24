package org.carl.infrastructure.authorization;

import io.smallrye.mutiny.Uni;
import java.util.Map;
import java.util.Set;

/** user identity */
public interface AuthIdentity {

    String USER_ATTRIBUTE = "auth.user.identity";

    boolean isAnonymous();

    Set<String> getRoles();

    boolean hasRole(String role);

    <T> T getAttribute(String name);

    Map<String, Object> getAttributes();

    Uni<Boolean> checkPermission(Permission permission);

    default boolean checkPermissionBlocking(Permission permission) {
        return checkPermission(permission).await().indefinitely();
    }
}
