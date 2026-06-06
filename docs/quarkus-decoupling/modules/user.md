# user

## 当前定位

`infrastructure-component-quarkus:user` 当前包含 `KeycloakAuthProvider` 和 `UserAuthorizationService`，直接依赖 Quarkus `SecurityIdentity` 与 authorization 能力。它更像用户身份适配层，而不是完整用户 core。

## 是否需要独立 core

暂不需要。用户模型与权限抽象应优先进入 `infrastructure-component-authorization`。只有出现独立用户目录、用户资料或组织关系服务时，再创建 user core。

## 建议独立模块名

暂不创建。可选：`infrastructure-component-user`，仅在用户领域能力独立成型后创建。

## Quarkus adapter 应保留内容

- `SecurityIdentity` 到 authorization core 用户身份的适配。
- Keycloak/OIDC claims 解析。
- 默认 `AuthProvider` Bean 注册。
- 与 Quarkus Security 相关的请求上下文访问。

## 需要迁出的核心能力

- 通用 `IUserIdentity` 应放在 authorization core。
- 与 Keycloak 无关的用户、组织、用户组模型应放在 authorization core 或未来 user core。
- 权限判断规则不应放在 user adapter，应调用 authorization core。

## 依赖边界

- Quarkus user adapter 可以依赖 Quarkus Security、authorization core 和 authorization Quarkus adapter。
- authorization core 不得依赖 user adapter。
- 如果未来创建 user core，不能依赖 `SecurityIdentity`、Keycloak adapter 或 Quarkus Arc。

## 拆分任务清单

- 等 authorization core 拆分后，调整 user 模块依赖 core `IUserIdentity`。
- 将 `SecurityIdentity` claims 解析封装为单向 adapter。
- 检查 `UserAuthorizationService` 是否包含权限判断规则，若有则迁回 authorization core。
- 保持 Keycloak 相关代码在 Quarkus user adapter。

## 验收标准

- `./gradlew :infrastructure-component-quarkus:user:test` 通过。
- user adapter 中不定义通用权限领域模型。
- authorization core 不依赖 user adapter。
- 所有 `SecurityIdentity` 使用都限定在 Quarkus adapter 层。

## 模块审查补充

**解决的问题**：把 Quarkus Security/Keycloak 的当前登录用户适配成基础设施里的授权身份，用于业务获取当前用户、判断超级用户或调用授权能力。

**如何使用**：依赖 `infrastructure-component-quarkus:user`，配置 `quarkus.plugins.user.enable=true`。业务侧通过 `KeycloakAuthProvider` 或 `UserAuthorizationService` 获取当前用户和权限相关信息，底层依赖 Quarkus `SecurityIdentity`。

**当前依赖**：`implementation(project(":infrastructure-component-quarkus:web"))`、`implementation(project(":infrastructure-component-quarkus:authorization"))`，测试依赖 Mockito。

**需要注意**：user 模块不应发展成权限 core。`SecurityIdentity`、Keycloak claims、当前请求用户读取留在 adapter；用户身份抽象和权限模型应归入 authorization core。审查时要防止 user 与 authorization 互相循环膨胀。
