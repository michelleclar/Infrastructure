# audit

## 当前定位

仓库中存在顶层目录 `infrastructure-component-audit`，但 `settings.gradle.kts` 当前未 include，并注明已迁到 `infrastructure-component-quarkus:audit`。Quarkus audit 模块内包含 `AuditContext`、`AuditEvent`、`IAuditOperations`、`IAuditAbility`、`AuditStd` 和 Bean 注册代码。

## 是否需要独立 core

需要整理已有顶层模块，使其成为纯审计 core。Quarkus 层只负责按开关注册审计能力。

## 建议独立模块名

`infrastructure-component-audit`

## Quarkus adapter 应保留内容

- `AuditService` 上的 Quarkus build property 条件注册。
- `AuditContextProvider` 的 CDI producer。
- 将 core `AuditContext` 注册为 Quarkus Bean 的装配代码。
- 后续如接入请求上下文、用户上下文或事务上下文，也只放 adapter。

## 需要迁出的核心能力

- `AuditEvent` 审计事件模型。
- `IAuditOperations`、`IAuditAbility`。
- `AuditContext` 和默认内存或空实现。
- `AuditStd` 中与 Quarkus 无关的标准能力委托。

## 依赖边界

- core 可以依赖日志组件和工具组件。
- core 不得包含 `META-INF/beans.xml` 作为必须条件，除非明确要支持 CDI 独立场景。
- adapter 可以依赖 Quarkus Arc，并决定是否通过 `quarkus.plugins.audit.enable` 注册 Bean。

## 拆分任务清单

- 恢复或重新 include `infrastructure-component-audit`。
- 对比顶层 audit 与 Quarkus audit 的源码，合并出唯一 core 版本。
- 从 Quarkus audit 中删除重复 core 代码，改为依赖顶层 audit。
- 将 provider、producer、build property 开关留在 adapter。
- 为 `AuditContext` 增加无需 Quarkus 的单元测试。

## 验收标准

- `./gradlew :infrastructure-component-audit:test` 通过。
- `./gradlew :infrastructure-component-quarkus:audit:test` 通过。
- 顶层 audit 模块源码中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。
- Quarkus audit 模块不再定义审计领域模型的重复副本。

## 模块审查补充

**解决的问题**：给业务和基础设施组件提供统一审计入口，用于记录操作、数据变更、安全事件或系统事件。当前问题是顶层 `infrastructure-component-audit` 和 Quarkus audit 之间存在历史迁移痕迹，模型与 provider 容易重复。

**如何使用**：在 Quarkus 应用中依赖 `infrastructure-component-quarkus:audit`，配置 `quarkus.plugins.audit.enable=true` 后注入 `IAuditAbility` 或 `IAuditOperations`。非 Quarkus 项目应使用恢复后的 `infrastructure-component-audit` core，而不是依赖 Quarkus audit。

**当前依赖**：Quarkus audit 依赖 `libs.quarkus.arc`；顶层 audit 当前目录存在但未 include，且也含 Quarkus Arc 依赖。

**需要注意**：先确认是否恢复顶层 audit include。审查时要把 `AuditEvent`、`AuditContext`、`IAuditOperations` 归为 core，把 `@Produces`、`@ApplicationScoped`、`@IfBuildProperty` 归为 Quarkus adapter。不要维护两份审计事件模型。
