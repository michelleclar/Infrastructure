# workflow-temporal

## 模块定位

`infrastructure-component-workflow-temporal` 是基于 Temporal 的配置驱动工作流运行时。它解释执行 `workflow-core` 生成的 `WorkflowDefinition`，提供通用 `GenericWorkflowImpl`、`WorkflowEngine` 门面、worker setup、业务 Activity 注册、signal、查询、归档和示例。

## 核心能力

- runtime：`GenericWorkflowImpl`、`GenericWorkflow`、`WorkflowEngine`、`WorkflowHandle`、`WorkerSetup`、`WorkflowInput`、`WorkflowResult`、`WorkflowState`。
- activity：`BusinessActivityRegistry`、`GenericActivity`、`GenericActivityImpl`、`ActivityCall`、`ActivityResult`。
- handler/interceptor holder：`HandlerHolder`、`InterceptorHolder`、`AsyncInterceptorActivity`。
- archive：`ArchiveActivities`、`DatabaseArchiveActivities`、`WorkflowInstanceSnapshot`。
- example：`QuickStartExample` 等快速启动示例。

## 依赖边界

- 依赖 Temporal SDK、`workflow-core`、`persistence-jooq` 和日志组件。
- 运行时需要 Temporal Server。
- PostgreSQL 归档是可选能力；也可以自实现 `ArchiveActivities`。
- 不属于 Quarkus 子模块，不负责 Quarkus CDI、ConfigMapping 或生命周期事件。

## 对外 API

- `WorkflowEngine.connect(EngineConfig.of(target, queue))`。
- `WorkflowEngine#withWorker(handlers, activities)`。
- `WorkflowEngine#start(definition, businessData)`。
- `WorkflowHandle#signal(...)`、`query()`、`awaitResult()`。
- `WorkerSetup.setup(...)`：手动 worker/client 写法时注册 handler、activity、archive、interceptor。

## 典型使用场景

- 业务方只写流程定义和业务 Activity，不写 Temporal 工作流代码。
- 使用 `WorkflowEngine` 门面隐藏 `io.temporal.*` API。
- 通过 signal 推进 approvalTask、userTask、eventTask。
- 使用 taskGroup 支持会签、或签和嵌套会签。
- 使用可选归档把 `WorkflowInstanceSnapshot` 写入 PostgreSQL 或自定义 sink。

## 维护事项

- 自定义 `NodeHandler` 的 `run`、`onEvent`、`compensate` 运行在 Temporal 工作流线程，必须是确定性函数，禁止 IO、时钟、随机、阻塞和可变共享状态。
- 需要副作用时返回 WAITING + `RuntimeIntents.ACTIVITY`，由 runtime 派发到 Activity。
- `BusinessActivityRegistry` 按名字注册 `Function<Map<String, Object>, Map<String, Object>>`，名称必须与节点配置一致。
- `ServiceTaskHandler` 不做占位符替换，`activityInput` 是字面 Map。
- 归档开关在 `WorkflowInput`，是 per-execution 配置。
- 异步 hook 失败不会影响主流程。

## 测试验收

- `./gradlew :infrastructure-component-workflow-temporal:test` 通过。
- 本地测试使用 Temporal `TestWorkflowEnvironment`。
- 远端真实 Temporal 测试使用 `TEMPORAL_TARGET`。
- 请假审批、taskGroup、ANY 短路、嵌套会签、回环、子工作流、Saga 补偿、条件路由、interceptor 等场景有测试示例。

## 使用与依赖补充

**为了解决什么**：把可序列化流程定义真正跑在 Temporal 上，统一 worker、Activity、signal、状态推进、拦截器和归档能力。

**如何使用**：依赖 `infrastructure-component-workflow-temporal`，注册 `BuiltInHandlers` 和业务 `BusinessActivityRegistry`，通过 `WorkflowEngine` 启动 worker 和流程，使用 `WorkflowHandle#signal(...)` 推进等待节点。

**当前依赖了什么**：`api(libs.temporal.sdk)`、`implementation(project(":infrastructure-component-log"))`、`implementation(project(":infrastructure-component-persistence-jooq"))`、`implementation(project(":infrastructure-component-workflow-core"))`。

**需要注意什么**：Temporal replay 对确定性非常敏感。不要在 handler 中直接访问外部系统；外部调用必须通过 Activity。运行前确认 Temporal target、namespace、taskQueue 与 worker 配置一致。
