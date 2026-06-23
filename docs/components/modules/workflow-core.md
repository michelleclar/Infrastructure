# workflow-core

## 模块定位

`infrastructure-component-workflow-core` 是纯 Java 工作流引擎核心库，包含流程定义、Flow DSL、NodeHandler SPI、内置节点 handler、图结构校验、路由、节点配置编解码和 interceptor SPI。它没有 Temporal 依赖。

## 核心能力

- definition：`WorkflowDefinition`、`NodeDefinition`、`EdgeDefinition`、`NodeResult`、`NodeStatus`、`ExecutionRecord`。
- DSL：`Flow`、`FlowDef`、`NodeBuilder`、`BuiltInNodes`、`Dsl`、`JoinSpec`。
- SPI：`NodeHandler`、`NodeHandlerRegistry`、`NodeType`、`NodeTypes`、`BuiltInNodeType`、`NodeSpec`、`NodeExecutionContext`、`WorkflowEvent`、`DeterminismGuard`、`ConditionEvaluator`。
- handlers：serviceTask、approvalTask、userTask、eventTask、timerTask、taskGroup、gateway、subProcess、endTask 等 9 个内置节点类型。
- graph：`WorkflowGraph`、`GraphValidator`、`ValidationReport`、`EdgeMatch`。
- engine：`EdgeRouter`、`NodeConfigCodec`。
- interceptor：`WorkflowInterceptor`、`DeterministicInterceptor`、`AsyncInterceptor`、`WorkflowInterceptorRegistry`。

## 依赖边界

- 不依赖 Temporal、Quarkus、CDI、JAX-RS、MicroProfile Config。
- 可以依赖日志组件、Jackson 和 Jakarta EL。
- 只定义流程、校验流程和扩展节点类型；实际运行在 Temporal 上时使用 `infrastructure-component-workflow-temporal`。

## 对外 API

- `Flow.define(id, name)`：创建流程定义。
- `flow.node(...)`、`flow.from(...).on(...).to(...)`：声明节点与路由。
- `flow.build()`：生成不可变 `WorkflowDefinition`。
- `NodeHandlerRegistry#register(...)`：注册自定义节点 handler。
- `GraphValidator.validate(...)`：校验流程拓扑和节点配置。

## 典型使用场景

- 离线编写和校验流程定义。
- 自定义 serviceTask、approvalTask 之外的节点类型。
- 在 UI、配置中心或测试中构造可序列化流程图。
- 为 Temporal runtime 提供 `WorkflowDefinition`。

## 维护事项

- 路由 key 大小写敏感，`.on("SUCCESS")` 必须与 handler 返回的 `outcome` 完全一致。
- 出边未匹配时 `EdgeRouter.pickNextEdge` 返回 `null`，runtime 会把当前节点视为终止节点。
- 业务 handler 必须用 `register(...)`，触发 `DeterminismGuard`；只有模块内置 handler 使用 `registerBuiltIn(...)`。
- `DeterminismGuard` 是 lint，不是运行时沙箱；它不递归分析委托类、接口调用链或 lambda。
- 需要 IO、时钟、随机时，handler 应返回 `NodeResult.waiting()` 并通过 `RuntimeIntents.ACTIVITY` 把副作用交给 runtime Activity。

## 测试验收

- `./gradlew :infrastructure-component-workflow-core:test` 通过。
- `GraphValidator` 覆盖节点/边引用、起点、终点、环、不可达节点、handler 解析和 config 绑定。
- 自定义 handler 的确定性约束和内置节点路由行为有测试覆盖。

## 使用与依赖补充

**为了解决什么**：把流程定义、节点模型、路由、条件表达式、会签/或签、图校验和节点扩展点从运行时中抽离，形成不绑定 Temporal 的工作流核心。

**如何使用**：依赖 `infrastructure-component-workflow-core`，使用 `Flow.define(...)` 声明流程，使用 `BuiltInNodes` 创建节点，用 `GraphValidator` 校验，再交给 runtime 或持久化保存。

**当前依赖了什么**：`implementation(project(":infrastructure-component-log"))`、Jackson BOM 和 `jackson-databind`、`jakarta.el-api:5.0.1`、`org.glassfish:jakarta.el:5.0.0-M1`。

**需要注意什么**：这是工作流定义层，不负责 Temporal worker、Activity 调度、signal 收发或归档；这些能力在 `workflow-temporal`。
