package org.carl.infrastructure.user;

import io.quarkus.security.identity.SecurityIdentity;

import org.carl.infrastructure.authorization.IUserIdentity;
import org.carl.infrastructure.authorization.ModuleStandardPermission;
import org.carl.infrastructure.authorization.modle.Permission;
import org.carl.infrastructure.authorization.modle.UserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthorizationServiceTest {

    @Mock
    SecurityIdentity securityIdentity;

    @Spy
    UserAuthorizationService service;

    @BeforeEach
    void setUp() throws Exception {
        var field = UserAuthorizationService.class.getDeclaredField("securityIdentity");
        field.setAccessible(true);
        field.set(service, securityIdentity);
    }

    @Test
    void getModule_returnsUser() {
        assertEquals("user", service.getModule());
    }

    @Test
    void getSecurityIdentity_returnsInjectedIdentity() {
        assertSame(securityIdentity, service.getSecurityIdentity());
    }

    @Test
    void getIdentity_returnsNonNullUserIdentity() {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getRoles()).thenReturn(new HashSet<>());
        when(securityIdentity.getAttributes()).thenReturn(new HashMap<>());

        IUserIdentity identity = service.getIdentity("some-token");

        assertNotNull(identity);
        assertInstanceOf(UserIdentity.class, identity);
    }

    @Test
    void check_returnsTrueWhenIdentityHasRequiredPermission() {
        Permission permission =
                Permission.PermissionBuilder.create("VIEW")
                        .addAction(
                                b -> b.enable(true)
                                        .addStandardAction(ModuleStandardPermission.VIEW)
                                        .build())
                        .build();
        Map<String, Set<Permission>> permissions = new HashMap<>();
        permissions.put(ModuleStandardPermission.VIEW.getName(), Set.of(permission));

        IUserIdentity identity =
                UserIdentity.UserIdentityBuilder.create()
                        .setAnonymous(false)
                        .setRoles(new HashSet<>())
                        .setAttributes(new HashMap<>())
                        .setPermissions(permissions)
                        .build();

        doReturn(identity).when(service).getIdentity();

        assertTrue(service.check(ModuleStandardPermission.VIEW));
    }
}
