package org.carl.infrastructure.authorization;

import org.carl.infrastructure.authorization.Model.ModuleBuild;
import org.junit.jupiter.api.Test;

class IModelTest {
    @Test
    void testModel() {
        Model userModule =
                ModuleBuild.create()
                        .setCode("user")
                        .setName("user module")
                        .setDescription("manager user")
                        .setUrl("/user")
                        .setIsVisible(true)
                        .setIsRoot(true)
                        .addPermissions(
                                builder ->
                                        builder.name("view user")
                                                .action("view")
                                                .description("view user info")
                                                .build())
                        .addPermissions(
                                builder ->
                                        builder.name("edit user")
                                                .action("edit")
                                                .description("edit user info")
                                                .build())
                        .addSubModules(
                                sub ->
                                        sub.setCode("user.profile")
                                                .setName("user profile")
                                                .setDescription("user profile info")
                                                .setUrl("/user/profile")
                                                .setIsVisible(true)
                                                .addPermissions(
                                                        b ->
                                                                b.name("view user profile")
                                                                        .action("read")
                                                                        .description(
                                                                                "view user profile info")
                                                                        .build())
                                                .addPermissions(
                                                        b ->
                                                                b.name("edit user profile")
                                                                        .action("update")
                                                                        .description(
                                                                                "edit user profile info")
                                                                        .build())
                                                .build())
                        .build();
    }
}
