package org.carl.infrastructure.component.authorization;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.authorization.ModulePermission;
import org.carl.infrastructure.authorization.ModuleStandardPermission;
import org.carl.infrastructure.authorization.modle.UserIdentity;
import org.carl.infrastructure.component.web.model.ApiRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class ModulePermissionTest {
    @Test
    public void modulePermission() {
        String requestUrl = "/api/v1/user.manager.employee/create";
        // /api/v1/{module-path}/{action}
        // e.g. /api/v1/user.manager.employee/create → module: user.manager.employee, action: create
        ApiRequest apiRequest = new ApiRequest(requestUrl);

        ModulePermission.ModulePermissionBuilder builder =
                ModulePermission.ModulePermissionBuilder.create();
        ModulePermission mp =
                builder.action(apiRequest.getAction()).name(apiRequest.getModuleName()).build();
        UserIdentity.UserIdentityBuilder userIdentityBuilder =
                UserIdentity.UserIdentityBuilder.create();
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
                                        .build());
        // current user permission: create
        Boolean flag = mp.hasPermission(userIdentityBuilder.build());
        assertTrue(flag);
    }
}
