package org.carl.infrastructure.user;

import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.authorization.AuthProvider;
import org.carl.infrastructure.authorization.IUserIdentity;
import org.carl.infrastructure.authorization.ModulePermission;
import org.carl.infrastructure.authorization.ResourceIPermission;
import org.carl.infrastructure.authorization.modle.UserIdentity;
import org.carl.infrastructure.component.web.constant.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keycloak 认证适配器
 *
 * <p>职责： - 从令牌解析并构建 {@link IUserIdentity} - 基于用户身份执行模块权限校验
 */
@ApplicationScoped
public class KeycloakAuthProvider implements AuthProvider {
    private static final Map<String, IUserIdentity> STORE = new ConcurrentHashMap<>();
    @Inject SecurityIdentity securityIdentity;

    /** 从 JWT 构建用户身份并缓存 */
    @Override
    public IUserIdentity getIdentity(String token) {
        IUserIdentity identity = new UserIdentity(securityIdentity);
        Object attribute = identity.getAttribute(Constants.USER_ID);
        if (attribute instanceof String userId) {
            STORE.put(userId, identity);
        }
        return identity;
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
