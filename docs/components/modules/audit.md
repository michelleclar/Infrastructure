# audit

## 模块定位

`infrastructure-component-audit` 是历史审计组件目录，当前目录存在，但 `settings.gradle.kts` 未 include，并有注释说明 audit 已迁到 `infrastructure-component-quarkus:audit`。当前源码内仍包含 `AuditContext`、`AuditEvent`、`IAuditOperations`、`IAuditAbility`，同时也混入了 Quarkus/CDI provider。

## 核心能力

- 审计事件模型 `AuditEvent`。
- 审计操作接口 `IAuditOperations`。
- 能力 mixin `IAuditAbility`。
- 审计上下文 `AuditContext`。
- 标准审计委托 `AuditStd`。

## 依赖边界

- 作为独立组件时，应移除 `io.quarkus.*`、`jakarta.enterprise.*`、`jakarta.inject.*`。
- CDI producer 与 `@IfBuildProperty` 应留在 `infrastructure-component-quarkus:audit`。
- 审计 core 可以依赖日志和工具组件，但不应依赖 Quarkus adapter。

## 对外 API

- `IAuditOperations`：审计操作入口。
- `IAuditAbility`：上下文或服务类暴露审计能力的 mixin。
- `AuditContext`：默认审计上下文。
- `AuditEvent`：审计事件载体。

## 典型使用场景

- 业务服务记录操作审计、数据变更审计或安全审计。
- Quarkus adapter 根据配置开关注入默认审计上下文。
- 未来可扩展数据库、MQ 或日志型审计 sink。

## 维护事项

- 先决定顶层 audit 是否恢复 include。如果恢复，必须将 Quarkus provider 迁回 Quarkus audit。
- 与 `docs/quarkus-decoupling/modules/audit.md` 保持一致，避免顶层 audit 与 Quarkus audit 双份模型漂移。
- 审计事件字段应保持稳定，新增字段要考虑持久化兼容。

## 测试验收

- 若恢复 include：`./gradlew :infrastructure-component-audit:test` 通过。
- `infrastructure-component-audit/src/main/java` 中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。
- `infrastructure-component-quarkus:audit` 只保留 Bean 注册、配置开关和运行时装配。

## 使用与依赖补充

**为了解决什么**：让审计能力可以脱离 Quarkus 被复用，避免审计事件模型和审计上下文只存在于 adapter 中。

**如何使用**：恢复 include 之前不建议新业务直接依赖该目录。完成整理后，非 Quarkus 项目依赖 `infrastructure-component-audit` 并使用 `AuditContext` 或 `IAuditOperations`；Quarkus 项目依赖 `infrastructure-component-quarkus:audit`，由 adapter 生产 core Bean。

**当前依赖了什么**：当前 build 文件依赖 `libs.quarkus.arc`，这说明它还不是纯 core。

**需要注意什么**：这是优先审查对象。先处理 include 状态和 Quarkus 注解迁移，再判断是否补审计 sink、持久化、异步写入等能力。
