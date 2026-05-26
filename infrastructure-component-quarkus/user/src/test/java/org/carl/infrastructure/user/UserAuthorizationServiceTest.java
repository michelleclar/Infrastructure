package org.carl.infrastructure.user;

import io.quarkus.security.identity.SecurityIdentity;

import org.carl.infrastructure.authorization.IUserIdentity;
import org.carl.infrastructure.authorization.ModuleStandardPermission;
import org.carl.infrastructure.authorization.modle.UserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthorizationServiceTest {

    @Mock
    SecurityIdentity securityIdentity;

    @InjectMocks
    UserAuthorizationService service;

    @BeforeEach
    void setUp() {
        // Mockito @InjectMocks wires securityIdentity into service automatically.
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
    void check_returnsFalseWhenIdentityHasNoPermissions() {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getRoles()).thenReturn(new HashSet<>());
        when(securityIdentity.getAttributes()).thenReturn(new HashMap<>());

        // UserIdentity built from SecurityIdentity has an empty permissions map,
        // so check() must return false for any required permission.
        assertFalse(service.check(ModuleStandardPermission.VIEW));
    }
}
