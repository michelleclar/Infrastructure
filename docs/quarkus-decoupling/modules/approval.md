# approval

## 当前定位

`infrastructure-component-quarkus:approval` 当前同时包含审批 REST API、审批请求/响应 DTO、审批模型、审批服务、jOOQ repository 和用户目录抽象。它已经超出 Quarkus adapter 的职责，属于 P1 拆分对象。

## 是否需要独立 core

需要。审批模型和审批流转规则不应依赖 Quarkus，应该作为独立审批组件复用。

## 建议独立模块名

`infrastructure-component-approval`

## Quarkus adapter 应保留内容

- `ApprovalResource` 等 JAX-RS 资源。
- Quarkus/CDI Bean 注册和 `@IfBuildProperty` 开关。
- 事务边界、请求身份提取、HTTP 错误到响应的转换。
- 基于 Quarkus 注入的 jOOQ `DSLContext` 或 `PersistenceContext` repository 实现。

## 需要迁出的核心能力

- `ApprovalInstance`、`ApprovalTask`、`ApprovalHistory`、`ApprovalNode` 等审批领域模型。
- `ActionType`、`ApprovalStatus`、`TaskStatus` 等枚举。
- 审批流转服务接口和不依赖框架的实现。
- `ApprovalRepository` 抽象接口。
- `UserContext`、`UserDirectory` 这类与审批业务有关但不绑定 `SecurityIdentity` 的抽象。
- 表结构常量或 schema 描述，如果未来希望 core 层定义 repository 契约。

## 依赖边界

- core 可以依赖 `infrastructure-component-dto`、`infrastructure-component-log`、`infrastructure-component-utils` 和必要的 Java 标准库。
- core 不得依赖 `jakarta.ws.rs.*`、`jakarta.enterprise.*`、`jakarta.inject.*`、`io.quarkus.*`。
- adapter 可以依赖 `infrastructure-component-approval`、`infrastructure-component-persistence-jooq`、`infrastructure-component-quarkus:persistence` 和 `infrastructure-component-quarkus:authorization`。

## 拆分任务清单

- 创建 `infrastructure-component-approval` 并加入 `settings.gradle.kts`。
- 将审批模型、枚举、服务接口迁入 core。
- 将当前 repository 拆成 core 接口和 Quarkus/jOOQ 实现。
- 将 REST 请求/响应 DTO 分层：HTTP 专用 DTO 留在 adapter，领域命令对象放入 core。
- 调整 `ApprovalResource` 只调用 core service，不直接承载审批规则。
- 为审批流转补充不依赖 Quarkus 的单元测试。

## 验收标准

- `./gradlew :infrastructure-component-approval:test` 通过。
- `./gradlew :infrastructure-component-quarkus:approval:test` 通过。
- `infrastructure-component-approval/src/main/java` 中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。
- Quarkus adapter 中保留的业务逻辑仅限请求转换、事务装配和 Bean 注册。

## 模块审查补充

**解决的问题**：提供通用审批流能力，包括发起审批、任务处理、审批历史、审批节点和审批实例状态管理。当前问题是 REST、服务规则、模型和 jOOQ repository 混在 Quarkus 模块里，导致非 Quarkus 项目无法复用审批核心。

**如何使用**：在 Quarkus 应用中依赖 `infrastructure-component-quarkus:approval`，配置 `quarkus.plugins.approval.enable=true` 后通过 `ApprovalResource` 暴露 HTTP API，业务侧也可以注入或调用 `ApprovalService` 发起和推进审批。审批数据访问当前依赖 `infrastructure-component-quarkus:persistence` 提供的 jOOQ 能力。

**当前依赖**：`implementation(libs.bundles.web)`、`implementation(project(":infrastructure-component-quarkus:persistence"))`，测试依赖 Mockito。

**需要注意**：审批模型、状态机式流转规则、repository 接口应先被识别为 core；REST request/response、JAX-RS 异常和事务边界留在 adapter。审查时重点看 `core` 包里是否存在 `@ApplicationScoped`、`@Inject`、`@IfBuildProperty` 或 JAX-RS 类型，这些都是拆分信号。
