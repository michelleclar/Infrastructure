package org.carl.infrastructure.authorization;

import static org.junit.jupiter.api.Assertions.*;

import org.carl.infrastructure.authorization.ModulePermission.ModulePermissionBuilder;
import org.carl.infrastructure.authorization.modle.UserIdentity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class ModulePermissionTest {
    @Test
    void testModulePermission() {
        // request: /api/v1/user.manager.employee/create
        // module: user.manager.employee action:create
        ModulePermissionBuilder builder = new ModulePermissionBuilder();
        ModulePermission mp = builder.action("create").name("user.manager.employee").description("create employee").build();
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setAnonymous(false).setRoles(Set.of("manager")).setPermissions(Map.of("user.manager.employee",Set.of()));
        // current user permission: edit
        Boolean flag = mp.hasPermission(userIdentity, ModuleStandardPermission.CREATE.name());
        assertTrue(flag);
        // current user permission: view
        flag = mp.hasPermission(userIdentity, ModuleStandardPermission.CREATE.name());
        assertFalse(flag);
    }
}
