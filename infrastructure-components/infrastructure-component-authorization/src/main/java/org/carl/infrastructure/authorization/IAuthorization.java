package org.carl.infrastructure.authorization;

// NOTE: rebac
public interface IAuthorization {
    /**
     * 检查当前用户是否有权限执行特定操作
     *
     * @param permission 权限对象
     * @return 如果有权限返回true，否则返回false
     */
    boolean hasPermission(Permission permission);

    /**
     * 检查当前用户是否拥有指定角色
     *
     * @param role 角色对象
     * @return 如果拥有该角色返回true，否则返回false
     */
    boolean hasRole(Role role);

    /**
     * 检查当前用户是否可以访问指定资源
     *
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param action 操作类型（如：read, write, delete等）
     * @return 如果可以访问返回true，否则返回false
     */
    boolean canAccess(String resourceType, String resourceId, String action);

    /**
     * 获取当前用户的所有角色
     *
     * @return 角色列表
     */
    java.util.List<Role> getRoles();

    /**
     * 获取当前用户的所有权限
     *
     * @return 权限列表
     */
    java.util.List<Permission> getPermissions();
}
