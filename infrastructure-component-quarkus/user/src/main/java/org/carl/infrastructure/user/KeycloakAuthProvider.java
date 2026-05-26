package org.carl.infrastructure.user;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.authorization.AuthProvider;
import org.carl.infrastructure.authorization.IUserIdentity;
import org.carl.infrastructure.authorization.ModulePermission;
import org.carl.infrastructure.authorization.ResourceIPermission;
import org.carl.infrastructure.authorization.modle.UserIdentity;

/**
 * Keycloak 认证适配器
 *
 * <p>职责： - 从令牌解析并构建 {@link IUserIdentity} - 基于用户身份执行模块权限校验
 *
 * <p>标注 {@link DefaultBean} 以避免与 {@link UserAuthorizationService}（同样满足 {@link AuthProvider}）
 * 产生 CDI AmbiguousResolutionException：当两者同时激活时，CDI 优先选择非默认 bean。
 */
@DefaultBean
@ApplicationScoped
@IfBuildProperty(name = "quarkus.plugins.user.enable", stringValue = "true")
public class KeycloakAuthProvider implements AuthProvider {
    @Inject SecurityIdentity securityIdentity;

    /** 从 JWT 构建用户身份（不缓存，SecurityIdentity 生命周期由 Quarkus OIDC 按请求管理） */
    @Override
    public IUserIdentity getIdentity(String token) {
        return new UserIdentity(securityIdentity);
    }

    @Override
    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }

    /** 按模块权限进行校验 */
    @Override
    public boolean hasModulePermission(ModulePermission permission) {
        return permission.hasPermission(getIdentity());
    }

    /** 资源级访问控制（REBAC/细粒度 ABAC）后续扩展 */
    @Override
    public boolean canAccess(ResourceIPermission permission) {
        return false;
    }
}
