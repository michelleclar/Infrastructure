package org.carl.infrastructure.authorization;

import static org.junit.jupiter.api.Assertions.*;

import org.carl.infrastructure.authorization.ModulePermission.ModulePermissionBuilder;
import org.carl.infrastructure.authorization.modle.UserIdentity.UserIdentityBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;

class ModulePermissionTest {
    @Test
    void testModulePermission() {
        // /api/v1/{module-path}/{action}
        // e.g. /api/v1/user.manager.employee/create → module: user.manager.employee, action: create
        ModulePermissionBuilder builder = new ModulePermissionBuilder();
        ModulePermission mp =
                builder.action("create")
                        .name("user.manager.employee")
                        .description("create employee")
                        .build();
        UserIdentityBuilder userIdentityBuilder = UserIdentityBuilder.create();
        // a manager user: userIdentity from token
        userIdentityBuilder
                .setAnonymous(false)
                .setRoles(Set.of("manager"))
                .addPermission(
                        "user.manager.employee",
                        permissionBuilder ->
                                permissionBuilder
                                        .addAction(
                                                f ->
                                                        f.enable(true)
                                                                .addStandardAction(
                                                                        ModuleStandardPermission
                                                                                .CREATE)
                                                                .build())
                                        .build())
                .addPermission(
                        "user.document",
                        permissionBuilder ->
                                permissionBuilder
                                        .addAction(
                                                f ->
                                                        f.enable(true)
                                                                .addStandardAction(
                                                                        ModuleStandardPermission
                                                                                .CREATE)
                                                                .build())
                                        .build());
        // current user permission: create
        Boolean flag = mp.hasPermission(userIdentityBuilder.build());
        assertTrue(flag);
    }
}
