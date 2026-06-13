# infrastructure-component-workflow-core

> 纯 Java 工作流引擎核心库。包含**领域模型**（WorkflowDefinition / NodeDefinition / EdgeDefinition / NodeResult）、**Flow DSL**（Flow / FlowDef / NodeBuilder / BuiltInNodes / Dsl / JoinSpec）、**NodeHandler SPI**（NodeHandler / NodeHandlerRegistry / BuiltInNodeType / DeterminismGuard）、9 个内置 Handler、**图结构校验**（WorkflowGraph / GraphValidator）和引擎辅助工具（EdgeRouter / NodeConfigCodec）。
>
> **零 Temporal 依赖**。只写流程定义、做离线校验或开发自定义节点类型：依赖本模块即可。要在 Temporal 上真正跑起来，见 [infrastructure-component-workflow-temporal](../infrastructure-component-workflow-temporal/README.md)。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-workflow-core"))
}
```

强制要求：**JDK 21**。

---

## 核心包/类概览

| 包 | 关键公开类型 | 职责 |
|---|---|---|
| `definition/` | `WorkflowDefinition`、`NodeDefinition`、`EdgeDefinition`、`NodeResult`、`NodeStatus`、`ExecutionRecord` | 不可变领域模型；JSON 可序列化（Jackson）；`WorkflowDefinition.of(...)` 提供无 startNodeId 的工厂方法 |
| `dsl/` | `Flow`、`FlowDef`、`NodeBuilder`、`BuiltInNodes`、`Dsl`、`JoinSpec`、`ChildNodeSpec`、`NodeConfig`、`FlowFrom`、`FlowJoin` | 流程图构建 DSL；`Flow.define(id, name)` 为入口；`flow.build()` 产出不可变 `WorkflowDefinition` |
| `spi/` | `NodeHandler<CONFIG,STATE,EVENT>`、`NodeHandlerRegistry`、`NodeType`、`NodeTypes`、`BuiltInNodeType`、`NodeSpec`、`NodeExecutionContext`、`WorkflowEvent`、`DeterminismGuard`、`ConditionEvaluator` | 节点类型扩展点；确定性合约强制执行；Jakarta EL 条件表达式求值 |
| `handlers/` | `BuiltInHandlers`（批量注册）、9 个 Handler 及其 Config record | 9 个内置节点类型的执行语义；`BuiltInNodeSpecs` 提供强类型 `NodeSpec<C>` 常量 |
| `graph/` | `WorkflowGraph`、`GraphValidator`、`ValidationReport`、`EdgeMatch` | O(1) 图索引；可达性分析；Tarjan 环检测；结构校验 |
| `engine/` | `EdgeRouter`、`NodeConfigCodec` | 与 Temporal 无关的纯函数路由决策；节点 config JSON 规范化 / 解码 |
| `interceptor/` | `WorkflowInterceptor`、`DeterministicInterceptor`、`AsyncInterceptor`、`InterceptorContext`、`WorkflowInterceptorRegistry` | 7 个生命周期 Hook 的 SPI；按 `order()` 排序注册 |

---

## Flow DSL 使用示例

### 基本用法

```java
import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.*;
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

// 1. 创建 FlowDef 累加器
FlowDef flow = Flow.define("leaveV2", "请假流程");
flow.start("requestLeave");   // 显式固定入口节点（有回边时必须）

// 2. 声明节点
flow.node("requestLeave", service("createLeaveRequest"));            // serviceTask 语法糖
flow.node("approval",     approval("manager"));                      // approvalTask 语法糖
flow.node("notify",       service("notifyManager"));
// endTask：使用 Lambda 写法，也可用 flow.endNode(name) 快捷方法
flow.node("completed",    b -> b.type(BuiltInNodeType.END_TASK).label("审批通过"));
flow.node("rejected",     b -> b.type(BuiltInNodeType.END_TASK).label("审批拒绝"));

// 3. 声明边（routing key 大小写敏感）
flow.from("requestLeave").on("SUCCESS").to("approval");
flow.from("approval").on("APPROVED").to("notify");
flow.from("approval").on("REJECTED").to("rejected");
flow.from("notify").on("SUCCESS").to("completed");

// 4. 构建不可变定义
WorkflowDefinition def = flow.build();
```

### 条件路由（Jakarta EL）

```java
// 同一 event 的多条出边按声明顺序匹配，第一个 when 为 true 的获胜；最后一条无 when 为默认分支
flow.from("requestLeave").on("SUCCESS")
    .when("${largeAmount}").to("bigApproval");   // variables 中的 boolean
flow.from("requestLeave").on("SUCCESS").to("normalApproval");   // 默认
```

EL 上下文顶层变量：

| 变量 | 类型 | 说明 |
|---|---|---|
| `${varName}` | 任意 | 裸标识符，从 `ctx.variables()` 取 |
| `${variables.x}` | `Map<String,Object>` | 等同 `${varName}` 的全路径形式 |
| `${businessData.a}` | `JsonNode` | 工作流启动时的业务数据 |
| `${results['nodeId'].outcome}` | `NodeResult` | 之前节点的执行结果 |
| `${ctx.variables}` / `${ctx.businessData}` / `${ctx.results}` | 聚合视图 | — |

### 会签 / 或签（taskGroup + join）

```java
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

// 全签：所有子任务通过后 -> APPROVED
flow.from("submit").on("SUCCESS").to("multiApproval");
flow.from("multiApproval")
    .join(all(
        node("hr",      approval("hr")),
        node("manager", approval("manager"))))
    .on("APPROVED").to("notify")
    .on("REJECTED").to("rejected");

// 或签：任意一个通过即短路 -> APPROVED
flow.from("submit").on("SUCCESS").to("orApproval");
flow.from("orApproval")
    .join(any(
        node("manager", approval("manager")),
        node("cfo",     approval("cfo"))))
    .on("APPROVED").to("notify");

// 嵌套：外层 ALL，内层一组 ANY
JoinSpec mgmt = any(
    node("manager", approval("manager")),
    node("cfo",     approval("cfo")));
flow.from("submit").on("SUCCESS").to("nestedApproval");
flow.from("nestedApproval")
    .join(all(
        node("hr",   approval("hr")),
        node("mgmt", new NodeConfig(NodeTypes.TASK_GROUP, Map.of()), mgmt)))
    .on("APPROVED").to("notify");
```

### 内置节点类型速查

| `BuiltInNodes` 工厂 / `BuiltInNodeType` 枚举 | `NodeTypes` 字符串 | 说明 |
|---|---|---|
| `service(activity)` / `SERVICE_TASK` | `"serviceTask"` | 同步调用业务 Activity；可选 `activityInput` 和 `compensateActivity` |
| `approval(assignee)` / `APPROVAL_TASK` | `"approvalTask"` | 等待 `approval`（或自定义）signal；出 `APPROVED`/`REJECTED`/`TIMEOUT` |
| `userTask(assignee)` / `USER_TASK` | `"userTask"` | 等待用户提交 signal |
| `event(awaitEvent)` / `EVENT_TASK` | `"eventTask"` | 等待指定 signal |
| `timer(iso8601Duration)` / `TIMER_TASK` | `"timerTask"` | 定时等待，如 `"PT1H"` |
| （`join(all/any(...))`）/ `TASK_GROUP` | `"taskGroup"` | 并行子任务 + 聚合规则 |
| — / `GATEWAY` | `"gateway"` | 纯路由分叉，无等待语义 |
| `subProcess(subWorkflowId)` / `SUB_PROCESS` | `"subProcess"` | 子工作流 |
| `flow.endNode(name)` / `END_TASK` | `"endTask"` | 终止节点 |

---

## 自定义节点 NodeHandler SPI

### 实现接口

```java
public interface NodeHandler<CONFIG, STATE, EVENT> {
    String type();                           // 节点类型字符串，必须稳定
    Class<CONFIG> configType();              // config JSON 绑定的 POJO 类

    // 默认返回 JsonNode；需要强类型时 override
    default Class<STATE> stateType() { ... }
    default Class<EVENT> eventType() { ... }

    // 进入节点时调用：返回 completed(outcome) 推进路由，或 waiting() 挂起等待
    NodeResult run(NodeExecutionContext ctx, CONFIG config);

    // 带类型化 state 的重载（默认委托上面的 run）
    default NodeResult run(NodeExecutionContext ctx, CONFIG config, STATE state);

    // 事件到达时的过滤器（cheap，不应有副作用）
    default boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, CONFIG config);

    // 带类型化 event payload 的重载
    default boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, CONFIG config, EVENT eventPayload);

    // 事件处理：canAccept 返回 true 后调用
    default NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, CONFIG config);
    default NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, CONFIG config, EVENT eventPayload);

    // Saga 补偿
    default boolean compensable() { return false; }
    default NodeResult compensate(NodeExecutionContext ctx, CONFIG config, NodeResult completedResult);
    default NodeResult compensate(NodeExecutionContext ctx, CONFIG config, NodeResult completedResult, STATE state);
}
```

### 最小示例

```java
// 配置 record
public record SmsConfig(String template, String to) {}

// Handler（实现必须确定性！）
public class SmsHandler implements NodeHandler<SmsConfig, JsonNode, JsonNode> {

    @Override public String type() { return "sms"; }

    @Override public Class<SmsConfig> configType() { return SmsConfig.class; }

    @Override
    public NodeResult run(NodeExecutionContext ctx, SmsConfig config) {
        // 不能直接调 smsClient.send()；把 IO 意图放进 payload，由 runtime 派发 Activity
        Map<String, Object> intent = Map.of(
            RuntimeIntents.ACTIVITY, "sendSms",
            RuntimeIntents.ACTIVITY_INPUT, Map.of(
                "to", config.to(),
                "template", config.template()));
        return new NodeResult(NodeStatus.WAITING, null, intent, null);
    }

    @Override
    public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, SmsConfig config) {
        return "_activityResult".equals(event.name());
    }

    @Override
    public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, SmsConfig config) {
        return NodeResult.completed("SUCCESS");
    }
}

// 注册（register 会触发 DeterminismGuard 静态扫描）
NodeHandlerRegistry registry = new NodeHandlerRegistry();
BuiltInHandlers.registerAll(registry);
registry.register(new SmsHandler());   // 用户 handler 必须用 register，不能用 registerBuiltIn
```

在 DSL 中引用自定义节点：

```java
// 写法 A：字符串 type
flow.node("notify", b -> b.type("sms").set("template", "你好").set("to", "13800000000"));

// 写法 B：定义枚举（推荐，编译期安全）
public enum MyNodeType implements NodeType {
    SMS;
    @Override public String value() { return "sms"; }
}
flow.node("notify", b -> b.type(MyNodeType.SMS).setAll(new SmsConfig("你好", "13800000000")));

// 写法 C：NodeSpec（类型 + config 一次性绑定，IDE 补全）
public static final NodeSpec<SmsConfig> SMS_SPEC = NodeSpec.of("sms", SmsConfig.class);
flow.node("notify", b -> b.config(SMS_SPEC, new SmsConfig("你好", "13800000000")));
```

### NodeExecutionContext 可读字段

| 方法 | 返回类型 | 说明 |
|---|---|---|
| `workflowId()` | `String` | 工作流实例 ID |
| `instanceId()` | `String` | 运行 ID（重试后变更） |
| `definitionId()` | `String` | 流程定义 ID |
| `currentNodeId()` | `String` | 当前节点 ID |
| `businessData()` | `JsonNode` | 工作流启动时的业务数据（只读） |
| `variables()` | `Map<String,Object>` | 可变工作流变量（只读视图） |
| `resultOf(nodeId)` | `NodeResult` | 之前已完成节点的结果，未执行返回 null |
| `currentEvent()` | `WorkflowEvent` | 当前事件（仅在 canAccept/onEvent 期间非 null） |
| `executionRecords()` | `List<ExecutionRecord>` | 全量执行历史（按执行顺序） |

### DeterminismGuard 合约

`NodeHandlerRegistry.register(handler)` 调用 `DeterminismGuard.assertPure(handlerClass)`，扫描字节码常量池，发现以下符号引用则抛 `IllegalStateException`：

**禁止的类型引用**（`FORBIDDEN_TYPES`）：

- `java.util.Date`、`java.time.Clock`、`java.util.UUID`、`java.util.Random`、`java.security.SecureRandom`
- `java.net.http.HttpClient`
- `java.io.File`、`java.io.FileInputStream`、`java.io.FileOutputStream`
- `java.sql.Connection`、`java.sql.DriverManager`

**禁止的方法调用**（`FORBIDDEN_METHODS`）：

- `java.time.Instant#now`、`java.time.LocalDateTime#now`、`java.time.LocalDate#now`
- `java.lang.System#currentTimeMillis`、`java.lang.System#nanoTime`
- `java.lang.Thread#sleep`
- `java.lang.Math#random`

> 这是 **lint，不是沙箱**：只检查 handler 类本身的常量池，不递归分析委托类、接口调用链或 lambda。真正的安全依赖代码审查；违反确定性合约会导致 Temporal Replay 崩溃。

需要 IO / 时钟 / 随机时：在 `run()` 返回 `NodeResult.waiting()` 并在 `payload` 中放入 `RuntimeIntents.ACTIVITY` 意图，让运行时把工作派发到 Activity。

### RuntimeIntents payload 键

| 常量 | 用途 |
|---|---|
| `RuntimeIntents.ACTIVITY` | 要调用的业务 Activity 名（String） |
| `RuntimeIntents.ACTIVITY_INPUT` | 传入 Activity 的参数（`Map<String,Object>`） |
| `RuntimeIntents.AWAIT_EVENT` | 等待的 signal 名（String） |
| `RuntimeIntents.ASSIGNEE` | 负责人标识（String） |
| `RuntimeIntents.TIMEOUT_DURATION` | ISO-8601 超时（String，如 `"PT24H"`） |
| `RuntimeIntents.DURATION` | timerTask 等待时长（String） |
| `RuntimeIntents.SUB_WORKFLOW_ID` | 子工作流 ID（String） |
| `RuntimeIntents.SET_VARIABLES` | 更新工作流变量（`Map<String,Object>`，在路由下一条边前生效） |

---

## Interceptor（生命周期 Hook）

7 个生命周期：`onWorkflowStart` / `onWorkflowEnd` / `onNodeEnter` / `onNodeExit` / `onNodeError` / `onEvent` / `onCompensate`。

两种 SPI：

| 接口 | 执行方式 | 适用场景 |
|---|---|---|
| `DeterministicInterceptor` | 工作流线程内联，**必须确定性** | in-memory 计数、确定性日志 |
| `AsyncInterceptor` | 运行时派发到 Activity，**可做 IO** | Kafka 推送、HTTP 回调、数据库写入 |

```java
WorkflowInterceptorRegistry interceptors = new WorkflowInterceptorRegistry();

// 确定性拦截器：仅覆盖关心的方法，其余保留 default no-op
interceptors.register(new DeterministicInterceptor() {
    @Override
    public void onNodeEnter(InterceptorContext ctx, NodeDefinition node) {
        // ctx 暴露：workflowId()、instanceId()、definitionId()、businessData()
    }
});

// 异步拦截器：IO 安全
interceptors.register(new AsyncInterceptor() {
    @Override
    public void onWorkflowEnd(InterceptorContext ctx, NodeResult terminalResult) {
        kafkaTemplate.send("workflow-events", terminalResult.outcome());
    }
});
```

`order()` 越小越先执行（默认 0）。实现类可同时 implement 两个接口，`WorkflowInterceptorRegistry.register()` 会分别归入对应列表。

---

## 校验（GraphValidator）

```java
// 仅结构校验（节点/边/起止节点/环）
ValidationReport report = GraphValidator.validate(definition);

// 结构校验 + handler 解析 + config 绑定
NodeHandlerRegistry registry = new NodeHandlerRegistry();
BuiltInHandlers.registerAll(registry);
ValidationReport report = GraphValidator.validate(definition, registry);

if (!report.ok()) {
    report.throwIfInvalid();   // 抛 IllegalStateException
}
// 或者手动查看
report.errors();    // List<String>  — 必须修复，不可执行
report.warnings();  // List<String>  — 建议检查（不可达节点、环、多起点等）
```

**GraphValidator 检查项：**

| 类型 | 内容 |
|---|---|
| **Error**（必须修复） | workflow id/name 空白；节点数为 0；节点 id 重复；节点 type 空白；边引用不存在的节点；explicit startNodeId 不存在；无起点（全环图）；多节点但无边；无终止节点 |
| **Error**（带 registry） | 节点 type 无对应 handler；节点 config 无法反序列化为 configType |
| **Warning**（可忽略） | 非 timerTask 的自环边；多个拓扑起点（未设 startNodeId）；不可达节点；有环 |

单节点 config 校验：

```java
List<String> errors = GraphValidator.validateNodeConfig(node, registry);
```

**WorkflowGraph 常用查询 API：**

```java
WorkflowGraph graph = new WorkflowGraph(definition);

graph.effectiveStartNode();          // 解析实际入口节点（优先 explicit > 拓扑唯一）
graph.startNodes();                  // 拓扑起点集合（无外部入边的节点）
graph.endNodes();                    // 终止节点集合（type=endTask 或无出边）
graph.outgoing(nodeId);              // 该节点所有出边
graph.incoming(nodeId);              // 该节点所有入边
graph.reachableFrom(nodeId);         // 从该节点可达的全部节点 id
graph.canReach(fromNodeId, toNodeId);
graph.detectCycles();                // List<List<String>>，每个 SCC 为一组 id
```

---

## 注意事项

1. **节点 id 重复即快速失败**：`FlowDef.node(name, ...)` 检测 id 冲突，重复声明抛 `IllegalStateException`；`WorkflowDefinition` 构造器本身不校验，交给 `GraphValidator`。

2. **路由 key 大小写敏感**：`EdgeRouter.pickNextEdge` 做精确字符串匹配。DSL `.on("SUCCESS")` 里的字符串必须与 handler `run()` 实际返回的 `outcome` 完全一致，拼写错误会导致流程静默终止于当前节点。

3. **出边未匹配时流程终止**：`EdgeRouter.pickNextEdge` 返回 `null` → runtime 把当前节点视为终止节点，不报错。

4. **`build()` 对未声明的边目标自动推断 endTask**：`.to("完成")` 如果没有对应 `flow.node("完成", ...)` 声明，`FlowDef.build()` 会把它自动注册为 `endTask` 类型节点。

5. **taskGroup 子节点不出现在顶层节点列表**：子节点配置被编码进 taskGroup 节点的 JSON config，不作为独立 `NodeDefinition` 存在。

6. **`register` vs `registerBuiltIn`**：只有模块内置 handler 可用 `registerBuiltIn`（绕过 DeterminismGuard）；业务 handler 必须用 `register`，会触发字节码静态扫描。

7. **Jackson 序列化**：`WorkflowDefinition`、`NodeDefinition`、`EdgeDefinition`、`NodeResult` 都标注 `@JsonInclude(NON_NULL)`，`null` 字段不输出，旧定义可安全反序列化（`@JsonIgnoreProperties(ignoreUnknown = true)`）。

8. **EL 表达式格式**：不以 `${` 开头或不以 `}` 结尾的字符串（除字面 `true`/`false`）会被直接视为 `false`，不会报错——避免把路由 key 写成 `when` 表达式。

---

## 交叉引用

要在 Temporal 上运行由本模块定义的 `WorkflowDefinition`，请参阅 [infrastructure-component-workflow-temporal/README.md](../infrastructure-component-workflow-temporal/README.md)。
