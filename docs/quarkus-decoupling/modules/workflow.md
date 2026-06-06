# workflow

## 当前定位

`infrastructure-component-quarkus:workflow` 当前包含 Temporal 配置、worker lifecycle、client interceptor、事务工作流构建器、workflow event/model、repository 和部分快照 DTO。它混合了 Temporal/状态机编排模型与 Quarkus 生命周期装配。

## 是否需要独立 core

需要。工作流编排模型、事件模型、构建器和 repository 契约应脱离 Quarkus。Quarkus 层只负责配置、生命周期、worker 注册和 Bean 装配。

## 建议独立模块名

优先：

- `infrastructure-component-workflow`

如果希望明确 Temporal 绑定：

- `infrastructure-component-workflow-temporal`

## Quarkus adapter 应保留内容

- `WorkflowArgsConfig` 配置映射。
- `WorkflowLifecycle` 对 `StartupEvent` 的监听。
- `WorkerManger` 中需要 CDI 注入和 Quarkus lifecycle 的部分。
- Quarkus Bean 注册、client producer、worker 注册。
- Quarkus 配置到 core/Temporal 配置对象的转换。

## 需要迁出的核心能力

- `ITransactionalWorkflow`、`TransactionalWorkflowImpl`。
- `TXWorkflowBuilder`、`TXWorkflowExecuter`、`TXWorkflowExecuterFactory`。
- `ActionWrapperFactory`、`GeneraterWorkflowStampId`。
- `WorkflowContext`、`WorkflowEvent`、`EntityEvent`、`EventTypeEnum`。
- `WorkflowRepository` 契约和快照/状态迁移 DTO。
- 与 `infrastructure-component-statemachine` 的集成逻辑。
- Temporal client interceptor 中不依赖 Quarkus 的部分。

## 依赖边界

- workflow core 可以依赖 Temporal SDK、`infrastructure-component-statemachine`、persistence-jooq 的接口或 repository 抽象。
- workflow core 不得依赖 Quarkus runtime、CDI、SmallRye Config、MicroProfile Config。
- Quarkus workflow adapter 可以依赖 Quarkus lifecycle、Arc、SmallRye Config 和 workflow core。
- 如果 repository 实现依赖 Quarkus 注入的 `DSLContext`，实现留在 adapter，接口留在 core。

## 拆分任务清单

- 创建 workflow core 模块，并决定是否命名为 `workflow` 或 `workflow-temporal`。
- 迁出事件模型、上下文、构建器、执行器和工作流接口。
- 将 `WorkflowArgsConfig`、`WorkflowLifecycle`、CDI 注入和 worker 注册保留在 Quarkus adapter。
- 将 repository 拆成 core 接口和 Quarkus/jOOQ 实现。
- 复用 `infrastructure-component-statemachine`，避免在 workflow 中复制状态机能力。
- 为工作流构建、事件生成、stamp id 和 repository 契约增加 core 测试。

## 验收标准

- `./gradlew :infrastructure-component-workflow:test` 或 `./gradlew :infrastructure-component-workflow-temporal:test` 通过。
- `./gradlew :infrastructure-component-quarkus:workflow:test` 通过。
- workflow core 源码中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。
- Quarkus workflow 中只保留配置、生命周期、worker 注册和 Bean 装配。

## 模块审查补充

**解决的问题**：提供 Temporal 工作流、事务工作流构建、状态机联动、事件和快照持久化能力，用于编排跨服务或跨步骤的业务流程。

**如何使用**：依赖 `infrastructure-component-quarkus:workflow`，配置 Temporal/Quarkus 相关连接和 `workflow.enable.log`。业务侧使用 `TXWorkflowBuilder`、`TXWorkflowExecuter` 或 `IStateMachineAbility` 组合状态机和 Temporal workflow。模块同时依赖 persistence 和 mq adapter。

**当前依赖**：`api(libs.quarkus.temporal)`、`api(project(":infrastructure-component-statemachine"))`、`implementation(project(":infrastructure-component-quarkus:persistence"))`、`implementation(project(":infrastructure-component-quarkus:mq"))`，测试依赖 `libs.quarkus.temporal.test`。

**需要注意**：Temporal SDK 相关模型不等于 Quarkus adapter；只有 lifecycle、ConfigMapping、worker 注册和 CDI 注入必须留在 Quarkus。审查时要把工作流模型、事件、构建器、执行器、repository 契约拆出，repository 的 jOOQ/CDI 实现再留 adapter。
