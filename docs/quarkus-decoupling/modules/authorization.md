# authorization

## 当前定位

`infrastructure-component-quarkus:authorization` 当前包含权限模型、用户身份抽象、权限定义、PDP/FGA client provider，以及直接依赖 `SecurityIdentity` 的适配代码。权限领域模型和判断规则不应绑定 Quarkus。

## 是否需要独立 core

需要。授权模型、权限接口和用户身份抽象属于跨框架能力，应该拆出为独立组件。

## 建议独立模块名

`infrastructure-component-authorization`

## Quarkus adapter 应保留内容

- `SecurityIdentity` 到 core `IUserIdentity` 的适配。
- Quarkus/CDI Bean 注册和默认 provider。
- MicroProfile Config 或 Quarkus 配置到 PDP/FGA client 配置对象的转换。
- 与 Quarkus Security、OIDC、Keycloak 相关的集成代码。

## 需要迁出的核心能力

- `IUserIdentity`、`IPermission`、`IModel`、`IModuleEnum` 等抽象。
- `ModulePermission`、`ModuleStandardPermission`、`ResourceIPermission`。
- `ModuleAction`、`Permission`、`UserGroup`、`UserOrganize` 等模型。
- 不依赖 `SecurityIdentity` 的 `UserIdentity` 实现。
- 权限判断服务接口和默认策略。
- 与 OpenFGA/PDP 无关的授权领域契约。

## 依赖边界

- core 可以依赖 `infrastructure-component-dto`、`infrastructure-component-log`、`infrastructure-component-utils`。
- 若保留 PDP 客户端，建议放到单独实现包或 `infrastructure-component-authorization-pdp`，避免 core 接口层被具体服务污染。
- adapter 可以依赖 `io.quarkus.security.identity.SecurityIdentity`，core 不得依赖。

## 拆分任务清单

- 创建 `infrastructure-component-authorization`。
- 将权限接口、领域模型和判断契约迁入 core。
- 将当前 `UserIdentity` 拆成 core 普通实现和 Quarkus `SecurityIdentity` adapter。
- 将 FGA/PDP provider 与配置读取留在 Quarkus adapter，或拆成独立 PDP 实现模块。
- 调整 `user` 模块依赖 core 授权接口，而不是反向耦合 authorization adapter 细节。
- 增加权限匹配、模块权限和资源权限的 core 单元测试。

## 验收标准

- `./gradlew :infrastructure-component-authorization:test` 通过。
- `./gradlew :infrastructure-component-quarkus:authorization:test` 通过。
- core 模块中没有 `SecurityIdentity`、Quarkus、CDI、JAX-RS、MicroProfile Config import。
- Quarkus adapter 仅负责身份适配、配置和 Bean 装配。

## 模块审查补充

**解决的问题**：提供用户身份、模块权限、资源权限和 PDP/FGA 集成入口，用于把业务请求转换成可判断的授权语义。当前问题是权限模型与 Quarkus `SecurityIdentity`、FGA provider 混在一起，导致权限 core 不够独立。

**如何使用**：Quarkus 应用依赖 `infrastructure-component-quarkus:authorization`，通过 `AuthProvider` 或 `IUserIdentity` 获取当前用户，通过 `IPermission`、`ModulePermission`、`ResourceIPermission` 等模型表达权限。需要 FGA 时配置 `fga.api-url`、`fga.store-id`、`fga.authorization-model-id`、`fga.api-token`。

**当前依赖**：`implementation(libs.bundles.auth)`，源码中使用 Quarkus Security `SecurityIdentity`、CDI 和 MicroProfile Config。

**需要注意**：`IUserIdentity`、权限模型和权限判断规则应迁到 core；`SecurityIdentity` 解析、FGA 配置读取和 Bean producer 留在 adapter。当前包名里存在 `modle` 拼写问题，若调整需要兼容迁移。
