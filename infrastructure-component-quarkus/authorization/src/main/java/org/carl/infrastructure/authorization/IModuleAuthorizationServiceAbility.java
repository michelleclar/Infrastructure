package org.carl.infrastructure.authorization;

/** domain authorization service */
public interface IModuleAuthorizationServiceAbility extends AuthProvider {
    <T extends Enum<T> & IModuleEnum> boolean check(T requiredPermission);

    default <T extends Enum<T> & IModuleEnum> boolean check(
            String permission, T requiredPermission) {
        return requiredPermission.getName().equalsIgnoreCase(permission);
    }

    String getModule();
}
