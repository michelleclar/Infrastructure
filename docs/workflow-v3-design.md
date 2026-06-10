# Workflow V3 Design

> 适用范围：`infrastructure-component-workflow-core` + `infrastructure-component-workflow-temporal`
> 状态：本文档反映**已落地的代码现状**（workflow-core 250+ 测试通过，workflow-temporal 端到端 demo 17 通过 / 0 失败），不是 aspirational
> 关系：取代 `docs/workflow-v2-design.md`；V2 的 POJO 形状被保留，新增 / 调整在每节明示

---

## 0. 演变脉络

| 版本 | 模型 | 主要问题 |
|------|------|----------|
| **V1** | 事件驱动状态机 + Temporal 持久化（`flow.from(A).to(B).on(EVENT).perform(activity)`）+ `approval(state)` 主流程 API | 审批被提升为主流程级 API；perform 绑在 transition 上语义混乱；流程定义和运行时强类型耦合 |
| **V2** | 流程图（nodes + edges）+ 可插拔 NodeHandler + outcome 路由 | 设计文档定稿但 DSL 对自定义 handler 封闭；没有 hook；表达式语言未选；condition/when 命名不一致；runtime 与 DSL 之间留有 G2 适配缝 |
| **V3**（本文档） | 在 V2 之上：DSL 通用化 / on=event 统一 / Interceptor SPI / Jakarta EL / Saga / Hook 派发常量；Temporal 适配层端到端跑通 | 仍有 4 项命名语义不一致（见 §13 待清理） |

---

## 1. 核心信条

固化的设计原则。后续修改如违背任一条须显式记录。

1. **业务感知不到 Temporal**：业务只写 `NodeHandler<C>.run(ctx, config) → NodeResult` 和 `Activity`。`@WorkflowInterface` / `Workflow.await` / `ActivityStub` 等 Temporal API 仅出现在 `infrastructure-component-workflow-temporal` 的 runtime 包内
2. **两层定位**：JSON 配置层（前端可视化）与 Java 编码层（强类型）共享一个 `WorkflowDefinition` POJO；两层的差异只在"怎么生成 POJO"，不在 POJO 自身
3. **业务自定义节点零侵入**：新增一个 NodeHandler 不需要修改 `workflow-core` 任何文件
4. **节点不直接决定下一节点**：节点产 `NodeResult(status, outcome, payload)`，路由由 `EdgeDefinition` 决定
5. **路由统一为 event 字段**：DSL `.on(name)` 写入 `EdgeDefinition.event`；节点完成时 runtime 把 outcome 视作内部 event 与 edge 匹配（G2 统一）
6. **节点产 payload，下游自取**：节点不直接 mutate 主 ctx；下游通过 `ctx.resultOf(prevId).payload()` 取
7. **确定性合同**：`NodeHandler` 和 `DeterministicInterceptor` 内不准 IO / 时间 / 随机 / 阻塞；IO 副作用通过 `RuntimeIntents` 派发给 runtime（runtime 自己跑在 Temporal Activity 里）
8. **依赖最小**：core 只用 Jackson；temporal 模块只新增 temporal-sdk + jakarta.el-api/glassfish jakarta.el；不引入 Spring/SpEL/statemachine/rule-engine/pdp

---

## 2. 模块边界

```
infrastructure-component-workflow-core   （纯 Java，0 Temporal 依赖）
  definition/   不可变 POJO + Jackson 注解
  spi/          SPI 抽象（NodeHandler / NodeExecutionContext / WorkflowEvent / Outcomes / NodeTypes / ConditionEvaluator / JsonNodeELResolver / ResultsELResolver / NodeHandlerRegistry / DeterminismGuard）
  handlers/     9 个内置 NodeHandler + 配置 record + RuntimeIntents
  dsl/          Java DSL（Flow / FlowDef / FlowFrom / FlowJoin / NodeBuilder / BuiltInNodes / Dsl / NodeConfig / ChildNodeSpec / JoinSpec）
  graph/        定义校验 + 可达性 + 边匹配（WorkflowGraph / GraphValidator / EdgeMatch / ValidationReport）
  interceptor/  Hook SPI（WorkflowInterceptor / DeterministicInterceptor / AsyncInterceptor / InterceptorContext / WorkflowInterceptorRegistry）

infrastructure-component-workflow-temporal   （依赖 workflow-core + temporal-sdk + 可选 jOOQ）
  runtime/      GenericWorkflow / GenericWorkflowImpl / GenericActivity / GenericActivityImpl
                BusinessActivityRegistry / HandlerHolder / ObjectMapperHolder / WorkerSetup
                WorkflowInput / WorkflowResult / WorkflowState / WorkflowInstanceSnapshot / ExecutionContext / ActivityCall / ActivityResult
  archive/      ArchiveActivities / DatabaseArchiveActivities（可选，按 WorkflowInput.archive 启用）
  example/      QuickStartExample（main 入口）
```

依赖方向严格单向：`workflow-temporal → workflow-core`，反向引用禁止。

---

## 3. 核心数据模型（不可变 record）

全部位于 `org.carl.infrastructure.workflow.definition`。Jackson 序列化采用 `@JsonInclude(NON_NULL)`，JSON ↔ POJO 双向同构。

### 3.1 WorkflowDefinition

```java
record WorkflowDefinition(
    String id,                       // 必填，流程定义唯一 ID
    String name,                     // 必填，显示名
    List<NodeDefinition> nodes,
    List<EdgeDefinition> edges,
    String startNodeId               // 可选；空时由 topology 推导
)
```

canonical 构造器做：
- `id` / `name` / `nodes` / `edges` 非空
- `nodes` / `edges` 用 `List.copyOf` 复制（强不可变）
- 若设 `startNodeId`，必须能在 `nodes` 里找到

### 3.2 NodeDefinition

```java
record NodeDefinition(
    String id,             // 必填，流程内稳定标识
    String label,          // 可选，UI 显示名（缺省回退到 id）
    String type,           // 必填，节点类型（serviceTask / approvalTask / ... / 业务自定义）
    String templateId,     // 可选，前端模板引用
    JsonNode config        // 可选，handler 解释的配置
)
```

### 3.3 EdgeDefinition

```java
record EdgeDefinition(
    String from,           // 必填
    String to,             // 必填
    String event,          // DSL .on(name) 写这里（V3 统一）
    String outcome,        // legacy 字段；新 DSL 不再写
    String when            // EL guard 表达式（V2 时叫 condition；V3 改名）
)
```

### 3.4 NodeResult / NodeStatus

```java
record NodeResult(
    NodeStatus status,                  // WAITING / COMPLETED / FAILED / CANCELLED
    String outcome,                     // 完成时的 outcome 名
    Map<String, Object> payload,        // 节点产物（含 RuntimeIntents）
    String message                      // 失败原因
)

enum NodeStatus { WAITING, COMPLETED, FAILED, CANCELLED }
```

静态工厂：`waiting()` / `completed(outcome)` / `completed(outcome, payload)` / `failed(message)` / `cancelled()`。

### 3.5 WorkflowEvent

```java
record WorkflowEvent(
    String name,
    JsonNode payload,
    String eventId          // 可空幂等键；非空时 signal() 据此去重，null/blank 不参与（详见 §9.6）
)
```

外部 signal 和内部合成事件用同一形状。保留 2 参兼容构造器 `WorkflowEvent(name, payload)`（eventId 默认 null），旧调用点与旧 JSON 零改动。

### 3.6 ExecutionRecord

每个节点访问的历史记录（含 back-edge 重访），用于回放和审计。

---

## 4. SPI

### 4.1 NodeHandler（`spi/NodeHandler.java`）

```java
public interface NodeHandler<C> {
    String type();                                            // 稳定的 type 标识
    Class<C> configType();                                    // 配置类型
    Set<String> outcomes();                                   // 可能产出的 outcome 闭集合（校验用）
    NodeResult run(NodeExecutionContext ctx, C config);       // 进入节点时调用一次

    default boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, C config);
    default NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, C config);
    default boolean compensable();
    default NodeResult compensate(NodeExecutionContext ctx, C config, NodeResult completedResult);
}
```

**确定性合同**（重要）：`run` / `onEvent` / `canAccept` 等所有方法**禁止** IO / 时间 / 随机 / 阻塞 / 读可变共享状态。需要副作用时，返回 `NodeResult.waiting()` 并通过 `RuntimeIntents` 告诉 runtime 该做什么。

### 4.2 NodeExecutionContext（`spi/NodeExecutionContext.java`）

handler 可见的只读上下文。所有方法确定性。

```java
public interface NodeExecutionContext {
    String workflowId();             // Temporal workflow id
    String instanceId();             // Temporal run id
    String currentNodeId();
    JsonNode businessData();         // 启动时的不可变输入
    Map<String, Object> variables(); // 不可变视图
    NodeResult resultOf(String nodeId);   // 查历史节点结果（下游"自取"）
    WorkflowEvent currentEvent();    // 仅 canAccept/onEvent 期间非 null
    default String definitionId();
    default List<ExecutionRecord> executionRecords();
}
```

### 4.3 NodeHandlerRegistry（`spi/NodeHandlerRegistry.java`）

```java
register(NodeHandler<?>)            // 用户路径：先过 DeterminismGuard.assertPure 字节码扫描
registerBuiltIn(NodeHandler<?>)     // 内置路径：跳过 guard
lookup(String type)                 // 抛 IllegalArgumentException 若缺
find(String type)                   // 返回 Optional
registeredTypes()                   // 已注册 type 集合
```

### 4.4 Interceptor SPI（`interceptor/` 包）

**两套接口**：

```java
interface WorkflowInterceptor {
    default int order() { return 0; }
}

interface DeterministicInterceptor extends WorkflowInterceptor {
    default void onWorkflowStart(InterceptorContext ctx);
    default void onWorkflowEnd  (InterceptorContext ctx, NodeResult terminalResult);
    default void onNodeEnter    (InterceptorContext ctx, NodeDefinition node);
    default void onNodeExit     (InterceptorContext ctx, NodeDefinition node, NodeResult result);
    default void onNodeError    (InterceptorContext ctx, NodeDefinition node, String errorMessage);
    default void onEvent        (InterceptorContext ctx, WorkflowEvent event);
    default void onCompensate   (InterceptorContext ctx, NodeDefinition node, NodeResult originalResult);
}

interface AsyncInterceptor extends WorkflowInterceptor { /* 同样 7 个 hook */ }
```

- `DeterministicInterceptor`：内联跑在 workflow 函数里，受确定性合同约束。适合：内存级 metric 计数、纯函数检查
- `AsyncInterceptor`：由 runtime 包装成 Temporal Activity 调用。可放心 IO。适合：审计、Kafka 投递、HTTP 回调

**注册**：`WorkflowInterceptorRegistry.register(interceptor)` 自动按 instanceof 分流到对应列表；同时实现两个接口允许；`order()` 升序返回。

**当前限制**：Hook 派发到 runtime 的具体机制（`RuntimeIntents.HOOK_PHASE` / `HOOK_PAYLOAD` / `HOOK_INTERCEPTORS` 常量已就绪）尚未在 `GenericWorkflowImpl` 调用。SPI 已建好，runtime 集成是 follow-up。

---

## 5. RuntimeIntents 协议

`org.carl.infrastructure.workflow.handlers.RuntimeIntents` 是 **handler ↔ runtime 之间的字符串键约定**——`NodeResult.payload` 用这些 key 携带"我想让 runtime 帮我做什么"。

| 常量 | key | 值类型 | 触发条件 |
|------|-----|--------|----------|
| `ACTIVITY` | `"activity"` | String | serviceTask 告诉 runtime 调哪个 Activity |
| `ACTIVITY_INPUT` | `"activityInput"` | Map | Activity 入参 |
| `ASSIGNEE` | `"assignee"` | String | approvalTask / userTask 的处理人 |
| `AWAIT_EVENT` | `"awaitEvent"` | String | approvalTask / userTask / eventTask 等待的事件名（三类型统一） |
| `TIMEOUT_DURATION` | `"timeoutDuration"` | ISO-8601 String | approval / eventTask 超时 |
| `DURATION` | `"duration"` | ISO-8601 String | timerTask 时长 |
| `CHILDREN` | `"children"` | List<Map> | taskGroup 子节点描述 |
| `JOIN_RULE` | `"joinRule"` | `"all"` / `"any"` | taskGroup 汇聚策略 |
| `SUB_WORKFLOW_ID` | `"subWorkflowId"` | String | subProcess 调起的子流程定义 id |
| `SUB_DEFINITION_JSON` | `"subDefinitionJson"` | String | subProcess 内联子流程定义 JSON |
| `SUB_INPUT` | `"subInput"` | Map | subProcess 入参 |
| `HOOK_PHASE` | `"hookPhase"` | String | hook 派发：WORKFLOW_START / NODE_ENTER / ... |
| `HOOK_PAYLOAD` | `"hookPayload"` | Map | hook 派发负载 |
| `HOOK_INTERCEPTORS` | `"hookInterceptors"` | List<String> | 指定要触发的 interceptor 类名 |

**核心机制**：

1. 业务/内置 `NodeHandler.run()` 返回 `NodeResult.waiting()` 并把意图写入 `payload`
2. runtime 从 `payload` 抠出 intent 常量
3. runtime 在 Temporal 上下文中执行真实副作用（调 Activity、awaitSignal、Async.invoke fan-out、newChildWorkflowStub 等）
4. 副作用完成后，runtime 合成内部事件（如 `_activityResult` / `_childCompleted` / `_timeout`）发回给 handler 的 `onEvent`
5. handler 据此决定 `NodeResult.completed(outcome)` 或继续 `waiting()`

这条机制是 core 零 Temporal 依赖的根。

---

## 6. 内置节点类型

定义在 `spi/NodeTypes.java`（type 字符串常量）+ `spi/Outcomes.java`（outcome 字符串常量）+ `handlers/` 包下 9 个 handler。

| type | 常量 | 配置 record | 产出 outcome | 触发的 RuntimeIntents |
|------|------|------------|--------------|----------------------|
| `serviceTask` | `SERVICE_TASK` | `ServiceTaskConfig` | `SUCCESS` / `FAILED` | `ACTIVITY` + `ACTIVITY_INPUT` |
| `approvalTask` | `APPROVAL_TASK` | `ApprovalTaskConfig` | `APPROVED` / `REJECTED` / `SENDBACK` / `TIMEOUT` | `ASSIGNEE` + `AWAIT_EVENT` + `TIMEOUT_DURATION` |
| `userTask` | `USER_TASK` | `UserTaskConfig` | `COMPLETED` / `CANCELLED` / `TIMEOUT` | 类似 approvalTask |
| `eventTask` | `EVENT_TASK` | `EventTaskConfig` | `RECEIVED` / `TIMEOUT` | `AWAIT_EVENT` |
| `timerTask` | `TIMER_TASK` | `TimerTaskConfig` | `TRIGGERED` / `CANCELLED` | `DURATION` |
| `taskGroup` | `TASK_GROUP` | `TaskGroupConfig` | `APPROVED` / `REJECTED` / `SENDBACK` / `COMPLETED` / `FAILED` / `TIMEOUT` / `CANCELLED` | `CHILDREN` + `JOIN_RULE` |
| `gateway` | `GATEWAY` | `GatewayConfig` | 任意（由 branches.outcome 决定） | 无（纯路由，run 内同步求值） |
| `subProcess` | `SUB_PROCESS` | `SubProcessConfig` | `COMPLETED` / `CANCELLED` / `FAILED` | `SUB_WORKFLOW_ID` / `SUB_DEFINITION_JSON` + `SUB_INPUT` |
| `endTask` | `END_TASK` | `EndTaskConfig` | （终端节点） | 无 |

`BuiltInHandlers.registerAll(registry)` 一次性注册全部 9 个。

### 6.1 taskGroup 子节点 key 约定

`TaskGroupContract.childKey(parentId, childShortId)` 返回 `"parent/short"`。

- runtime 启动每个子任务时用这个 key 作为 `currentNodeId`
- 子任务的 `NodeResult` 在 `WorkflowResult.nodeResults` 里以这个 key 索引
- 测试断言："approvals/hrApproval" 是 HR 子任务的完整 id

### 6.2 Saga 补偿

`NodeHandler.compensable()` 返回 `true` 的节点，**成功完成**后会被 runtime 推入补偿栈。当后续节点失败时，runtime 反向遍历栈，对每个补偿节点：
- 调 `handler.compensate(ctx, config, completedResult)`
- 如返回 WAITING + `ACTIVITY` intent，跑补偿 Activity
- 失败被 log + skip，不阻断后续补偿（best-effort）

`ServiceTaskHandler` 默认 `compensable() = true`；若 `ServiceTaskConfig.compensateActivity()` 非空，`compensate` 走 ACTIVITY intent；否则 no-op。

---

## 7. DSL

V3 的核心改进：**业务自定义 handler 零侵入接入 DSL**。

### 7.1 入口 + builder（`dsl/NodeBuilder.java`）

```java
public final class NodeBuilder {
    public NodeBuilder type(String type);              // 原生字符串
    public NodeBuilder type(NodeType nodeType);        // 类型化包装（推荐）
    public NodeBuilder label(String label);
    public NodeBuilder set(String key, Object value);  // 单 key-value
    public NodeBuilder setAll(Map<String, Object>);    // 批量 Map 形式
    public NodeBuilder setAll(Object pojo);            // 类型化 POJO（推荐）
}

// FlowDef 上的 builder 形 node 方法
public FlowDef node(String id, Consumer<NodeBuilder> configurer);
```

新增/自定义节点类型**不需要改 `workflow-core`**。三种写法力度递增：

```java
// 写法 1：纯字符串（最自由，无 IDE 帮助，仅适合 JSON 驱动场景）
flow.node("智能审核", b -> b
    .type("aiReview")
    .set("model", "gpt-4")
    .set("threshold", 0.85));

// 写法 2：业务声明 NodeType + 自由 set（typed type，untyped config）
public enum MyNodeType implements NodeType {
    AI_REVIEW("aiReview");
    private final String value;
    MyNodeType(String v) { this.value = v; }
    @Override public String value() { return value; }
}

flow.node("智能审核", b -> b
    .type(MyNodeType.AI_REVIEW)         // typed type
    .set("model", "gpt-4")              // 还是字符串 key
    .set("threshold", 0.85));

// 写法 3：业务声明 NodeType + typed config record（端到端 typed，**推荐**）
public record AiReviewConfig(String model, double threshold) {}

flow.node("智能审核", b -> b
    .type(MyNodeType.AI_REVIEW)                                // typed
    .setAll(new AiReviewConfig("gpt-4", 0.85)));               // typed
```

`NodeType` 是单方法函数式接口 `value() -> String`，业务可以用 enum / class / `NodeType.of(string)` 三种方式实现。

`setAll(Object pojo)` 用 Jackson 把 POJO 转 `Map<String, Object>` 合并进 props——任何 Jackson 能序列化的 record/bean 都可以。

业务想给自定义类型加 sugar：

```java
public final class MyNodes {
    public static Consumer<NodeBuilder> aiReview(String model, double threshold) {
        return b -> b.type(MyNodeType.AI_REVIEW)
                     .setAll(new AiReviewConfig(model, threshold));
    }
}
```

### 7.1.1 内置类型的 typed 入口（`spi/BuiltInNodeType.java`）

```java
public enum BuiltInNodeType implements NodeType {
    SERVICE_TASK("serviceTask"),
    APPROVAL_TASK("approvalTask"),
    USER_TASK("userTask"),
    EVENT_TASK("eventTask"),
    TIMER_TASK("timerTask"),
    TASK_GROUP("taskGroup"),
    GATEWAY("gateway"),
    SUB_PROCESS("subProcess"),
    END_TASK("endTask");
    // ...
}
```

`NodeTypes.X` 字符串常量保留兼容（JSON 驱动、运行时字符串比较仍需要），**新代码用 `BuiltInNodeType.X`**。两者由 `BuiltInNodeTypeAlignmentTest` 强制对齐。

### 7.2 BuiltInNodes（`dsl/BuiltInNodes.java`）

内置 sugar，返回 `Consumer<NodeBuilder>`。可选用——业务用 `.type(...).set(...)` 直接写也行。

```java
BuiltInNodes.service(String activity)
BuiltInNodes.service(String activity, Map<String, Object> activityInput)  // 双参重载，省去 .andThen
BuiltInNodes.approval(String assignee)
BuiltInNodes.userTask(String assignee)
BuiltInNodes.event(String awaitEvent)
BuiltInNodes.timer(String iso8601Duration)
BuiltInNodes.gateway()
BuiltInNodes.subProcess(String subWorkflowId)
BuiltInNodes.endTask()
BuiltInNodes.taskGroup()
```

单参 `service` 需要传额外 key 时用 `Consumer.andThen`；双参 `service` 直接内联 activityInput：

```java
// 单参 + andThen（仍有效）
flow.node("submit", BuiltInNodes.service("createOrder")
    .andThen(b -> b.set("activityInput", Map.of("customerId", "alice"))));

// 双参（推荐）
flow.node("submit", BuiltInNodes.service("createOrder", Map.of("customerId", "alice")));
```

### 7.3 Flow / FlowDef / FlowFrom / FlowJoin

```java
FlowDef flow = Flow.define("orderV2", "下单流程");
flow.start("createOrder");

flow.node("createOrder", BuiltInNodes.service("createOrder"));
flow.node("approve", BuiltInNodes.approval("manager")
    .andThen(b -> b.set("awaitEvent", "approveOrder")));
flow.node("done", b -> b.type(NodeTypes.END_TASK).label("已完成"));

flow.from("createOrder").on(Outcomes.SUCCESS).to("approve");
flow.from("approve").on(Outcomes.APPROVED).when("${ctx.variables.flag == true}").to("done");
flow.from("approve").on(Outcomes.APPROVED).to("alt-done");
// .on() 写入 EdgeDefinition.event
// .when() 写入 EdgeDefinition.when
// EdgeDefinition.outcome 字段保留兼容但新 DSL 不写

WorkflowDefinition def = flow.build();
```

### 7.4 Dsl（`dsl/Dsl.java`）—— 仅 join 结构性 API

```java
Dsl.all(ChildNodeSpec...)            // JoinSpec(rule="all")
Dsl.any(ChildNodeSpec...)            // JoinSpec(rule="any")
Dsl.node(String, NodeConfig)         // ChildNodeSpec（POJO 形式）
Dsl.node(String, Consumer<NodeBuilder>)         // ChildNodeSpec（builder 形式）
Dsl.node(String, NodeConfig, JoinSpec)          // 带 nestedJoin
Dsl.node(String, Consumer<NodeBuilder>, JoinSpec)
```

`Dsl.service/approval/end/taskGroup` 在 V3 **已删**（迁移到 `BuiltInNodes`）。

### 7.5 join / taskGroup 组合

```java
flow.from("createOrder").on(Outcomes.SUCCESS).to("approvals");
flow.from("approvals").join(Dsl.all(
        Dsl.node("hrApproval", BuiltInNodes.approval("hr")),
        Dsl.node("managerApproval", BuiltInNodes.approval("manager"))
)).on(Outcomes.APPROVED).to("ship")
  .on(Outcomes.REJECTED).to("cancel");
```

`flow.from("approvals").join(...)` 会自动把 `approvals` 注册为 `taskGroup` 类型节点。

### 7.6 嵌套 join（POJO 层就绪，runtime 暂未启用）

`ChildNodeSpec` 新增 `nestedJoin` 字段：

```java
record ChildNodeSpec(String name, NodeConfig config, JoinSpec nestedJoin)
```

POJO / JSON 层可以表达任意层级嵌套。`FlowDef.buildTaskGroupConfig` 递归把 `nestedJoin != null` 的子节点序列化为 `type="taskGroup"` + 内嵌 config；运行时通过 `executeChildSafely` → `registry.lookup("taskGroup")` → `TaskGroupHandler` → `driveTaskGroup` 链路天然递归，无需特殊处理。2 层端到端 + 3 层 JSON 已验证。

---

## 8. 表达式引擎

### 8.1 ConditionEvaluator（`spi/ConditionEvaluator.java`）

公开 API：

```java
ConditionEvaluator.evaluate(String expression, NodeExecutionContext ctx);          // null/blank → false
ConditionEvaluator.evaluateOrTrue(String expression, NodeExecutionContext ctx);    // null/blank → true
```

后端实现：**Jakarta EL 5（JSR-341）**。依赖：`jakarta.el:jakarta.el-api:5.0.1` + `org.glassfish:jakarta.el:5.0.0-M1`。

### 8.2 暴露给 EL 的顶层变量

| 变量 | 类型 | 来自 |
|------|------|------|
| `variables` | `Map<String, Object>` | `ctx.variables()` |
| `businessData` | `JsonNode` | `ctx.businessData()` |
| `results` | `ResultsView` | 懒查询 `ctx.resultOf(nodeId)` |
| `ctx` | `CtxView` | 聚合 view，含 `ctx.variables` / `ctx.businessData` / `ctx.results` |

### 8.3 自定义 ELResolver

- `JsonNodeELResolver`：把 `JsonNode` 当 bean 解析，`get(property)` 转 Java 类型（boolean / double / text / 嵌套 JsonNode）
- `ResultsELResolver`：`results.nodeId` → `NodeResult`，`NodeResult.outcome / status / payload / message` 支持点访问

### 8.4 语法示例

```java
"${ctx.businessData.amount > 1000}"
"${ctx.variables.role == 'admin'}"
"${ctx.results['hrApproval'].outcome == 'APPROVED'}"
"${variables.x > 5 && variables.y < 10}"
"${empty variables.missingKey}"
"${businessData.user.role == 'admin'}"
"${results.checkBalance.payload.balance >= 500}"
"${largeAmount}"                     // 单标识符兼容：等价 ${variables.largeAmount}
```

顶层 `"true"` / `"false"` 字面量也接受。任何解析或求值异常返回 `false`（保持宽容语义）。

---

## 9. Temporal 适配层

### 9.1 GenericWorkflow（`runtime/GenericWorkflow.java`）

唯一的 Temporal `@WorkflowInterface`：

```java
@WorkflowInterface
public interface GenericWorkflow {
    @WorkflowMethod WorkflowResult execute(WorkflowInput input);
    @SignalMethod void signal(WorkflowEvent event);
    @QueryMethod WorkflowState query();
}
```

业务代码**永远不写** `@WorkflowInterface`——所有流程跑在这一个通用 workflow 上，行为由入参 `WorkflowDefinition` 决定。

### 9.2 WorkflowInput / WorkflowResult / WorkflowState

```java
record WorkflowInput(
    WorkflowDefinition workflowDefinition,
    JsonNode businessData,
    Map<String, Object> initialVariables,
    String startNodeId)

record WorkflowResult(
    String finalNodeId,
    String finalOutcome,
    NodeStatus finalStatus,
    Map<String, NodeResult> nodeResults,
    List<ExecutionRecord> executionRecords,
    Map<String, Object> finalVariables)

record WorkflowState(
    String currentNodeId,
    boolean finished,
    Map<String, NodeResult> nodeResults,
    String lastEventName)
```

`WorkflowInput.from(definition, businessData)` 是便捷工厂，自动处理 `null` businessData 兼容。

### 9.3 GenericWorkflowImpl 的执行循环

```
1. 校验定义 (GraphValidator.validate)
2. 构建 WorkflowGraph
3. 解析 startNodeId
4. 进入循环：
   a. 取当前节点定义
   b. 反序列化 config 到 handler.configType()
   c. 调用 handler.run(ctx, config)
   d. 根据 NodeResult.status 分支：
      - COMPLETED + 有 outcome → pickNextEdge → 进入下一节点
      - COMPLETED + 无 outcome → 寻找无 event/outcome/when 的默认 edge
      - WAITING → 读 payload 的 RuntimeIntents 派发副作用（Activity / Timer / Signal await / Async fan-out / Child workflow）
        副作用完成 → 合成内部 event → 调 handler.canAccept + handler.onEvent → 循环
      - FAILED → 触发 saga 补偿（反向遍历 compensationStack）→ 终止
      - CANCELLED → 终止
   e. 节点 compensable 且 COMPLETED → 推入 compensationStack
5. 终止：返回 WorkflowResult；若 `WorkflowInput.archive=true` 则 `Async.procedure` 调用 `ArchiveActivities.archive` （fire-and-forget；失败吞错，不影响主流程结果）
```

### 9.4 pickNextEdge 的路由策略（G2 适配后）

`pickNextEdge(graph, currentNodeId, outcome, ctx)`：

```
若 outcome != null:
    1. 按 EdgeMatch.byEvent(outcome) 查 outgoing edges（V3 主路径：DSL .on() 写 event）
       命中后用 ConditionEvaluator 求 when 表达式；first match wins
    2. 兜底按 EdgeMatch.byOutcome(outcome) 查（legacy：手构 EdgeDefinition 直接写 outcome）

无 outcome 或上面都未命中：
    3. 找第一条 event/outcome/when 全 null 的"默认" edge

仍无命中：
    返回 null → workflow 终结于当前节点
```

### 9.5 Signal 收发

`signal(WorkflowEvent)` 入队 `signalQueue: Deque<WorkflowEvent>`。runtime 主循环在 `Workflow.await` 等待时被唤醒，取出 event 调 `handler.canAccept + handler.onEvent`。

### 9.6 Signal 事件幂等

#### WorkflowEvent.eventId 可空幂等键

`WorkflowEvent` 是 3 分量 record：`WorkflowEvent(String name, JsonNode payload, String eventId)`。

- `eventId` **可空**：非 null 且非 blank 时参与去重；null 或 blank 时不参与（行为与旧版完全相同）。
- 保留 2 参兼容构造器 `WorkflowEvent(name, payload)`，`eventId` 默认 `null`。
- `@JsonInclude(NON_NULL)` 保证旧 JSON（无 `eventId` 字段）反序列化为 `null`，**向后兼容**。

#### signal() 入队前去重

`GenericWorkflowImpl` 持有字段：

```java
private final Set<String> processedEventIds = new HashSet<>();
```

`signal(WorkflowEvent event)` 执行时，若 `event.eventId()` 非 null 且非 blank，则：
- 若 `processedEventIds` 已含该 id，**丢弃**（不入 `signalQueue`，不消费）；
- 否则 `processedEventIds.add(eventId)` 后正常入队。

`eventId` 为 null/blank 时跳过上述逻辑，直接入队——不影响不携带幂等键的旧 signal。

#### 确定性 / replay 安全

`processedEventIds` 是 workflow 状态的一部分，存在于 workflow 函数的局部内存中。Temporal 在 worker 重启或抢占时按 event history **确定性重放** `signal()` 调用；重放过程中集合会被逐条 signal 记录完整重建，最终与原始执行一致。实现只做 `Set.add` / `Set.contains`，不依赖 HashSet 的迭代顺序，满足 Temporal 的确定性合同。

#### 两种"重复"的区分

| 重复来源 | 是否需要应用层去重 | 处理方式 |
|----------|--------------------|---------|
| worker 重启 / 抢占后 Temporal 重放 | **不需要** | Temporal event history 天然保证每条 signal 只"生效"一次 |
| 客户端重发（网络重试 / 用户重复点击 / 上游重复调用） | **需要** | `eventId` 去重堵上这一 gap |

在 `eventId` 引入之前，客户端重发的同一条 signal 会被当作两次独立 signal 分别入队并消费，这是真实的语义漏洞。

#### 回环场景

若流程有回环（如审批被 rejected 后回到等待 approval 的节点），未去重时滞留在 `signalQueue` 中的重复 signal 可能在下一轮等待中被误消费。加入 `eventId` 去重后，已处理过的 eventId 在整个 workflow 生命周期内不会重复入队，消除了这种跨轮次误消费。

### 9.7 等待语义对应表

| `RuntimeIntents` | runtime 操作 | 完成时合成事件 |
|------------------|-------------|---------------|
| `ACTIVITY` + `ACTIVITY_INPUT` | `Workflow.newActivityStub(GenericActivity).invoke(...)` | `_activityResult` |
| `AWAIT_EVENT` + `TIMEOUT_DURATION` | `Workflow.await(timeout, () -> 收到 signal)` | signal name（成功）/ `_timeout`（超时） |
| `DURATION` | `Workflow.sleep(...)` | `_timer` |
| `CHILDREN` + `JOIN_RULE` | 每个 child `Async.function(...)` 启动，`Promise.allOf/anyOf` 等齐 | `_childCompleted`（逐个） |
| `SUB_WORKFLOW_ID` 或 `SUB_DEFINITION_JSON` | `Workflow.newChildWorkflowStub(GenericWorkflow).execute(...)` | `_subProcessCompleted` |

### 9.8 WorkerSetup

```java
WorkerSetup.setup(worker, handlerRegistry, activityRegistry);
WorkerSetup.setup(worker, handlerRegistry, activityRegistry, archiveActivities);  // 可选启用归档
WorkerSetup.setup(worker, handlerRegistry, activityRegistry, interceptorRegistry); // 可选启用 hook
WorkerSetup.setup(worker, handlerRegistry, activityRegistry, archiveActivities, interceptorRegistry);
```

注册 GenericWorkflow + GenericActivity 实现到 worker；可选注册 archive activity（实际是否调用由 per-execution `WorkflowInput.archive` 决定，不再是全局 static）；可选注册 `AsyncInterceptorActivity` impl 处理异步 hook。

### 9.9 Interceptor 集成（Hook 派发）

业务通过 `WorkflowInterceptorRegistry.register(interceptor)` 注册任意数量的 `DeterministicInterceptor` 或 `AsyncInterceptor`（同一对象可同时实现两个接口）。`InterceptorHolder` 是进程级单例 + 默认空 registry（zero-overhead default）。

**主循环接入的 5 个 phase**：

| Phase | 调用位置 | 派发方式 |
|-------|---------|---------|
| `WORKFLOW_START` | `startedAt` 赋值后、`resolveStartNode` 之前 | Deterministic 内联 + Async Activity |
| `NODE_ENTER` | `executeNode()` 调用之前 | 同上 |
| `NODE_EXIT` | `ctx.recordResult()` 之后 | 同上 |
| `NODE_ERROR` | FAILED / CANCELLED 分支，`runCompensation()` 之前 | 同上 |
| `WORKFLOW_END` | 三处 `return result` 之前（FAILED 终止、END_TASK 终止、无出边终止） | 同上 + 等齐 async promises |

**未接入**：`EVENT` 和 `COMPENSATE`（触发点在 `driveAwait` / `runCompensation` 内，注入风险较高，留 F2）。

**确定性合同**：
- Deterministic interceptor 在 workflow 函数内联执行，**异常被捕获 + log warn + swallow**，不会杀掉工作流
- Async interceptor 经由 `Workflow.newActivityStub(AsyncInterceptorActivity.class).invoke(AsyncHookInvocation)` 派发到 Activity，每次 hook 触发产生一个 `Promise<Void>`，收集到 `asyncHookPromises` 列表
- 每个 `return result` 之前 `Promise.allOf(asyncHookPromises).get()` 等齐，确保 workflow 终止前所有 hook 持久化
- Activity 端逐个调用 `AsyncInterceptor`，单个抛错 log + continue，不影响其他 interceptor

**注册示例**：

```java
WorkflowInterceptorRegistry interceptorRegistry = new WorkflowInterceptorRegistry();
interceptorRegistry.register(new MyMetricsInterceptor());        // Deterministic 计数器
interceptorRegistry.register(new MyAuditAsyncInterceptor());     // Async 写库

WorkerSetup.setup(worker, handlerRegistry, activityRegistry, interceptorRegistry);
```

### 9.10 BusinessActivityRegistry

业务通过 string 名注册 Activity：

```java
BusinessActivityRegistry registry = new BusinessActivityRegistry();
registry.register("createOrder", input -> {
    // 业务逻辑 — 可以放心 IO
    return Map.of("orderId", "ORD-" + UUID.randomUUID());
});
```

runtime 收到 `ACTIVITY` intent 时按 name 找业务函数调用。

---

## 10. 端到端示例：请假会签 ALL

```java
// 1. 定义（Java DSL，等价 JSON 配置层）
static WorkflowDefinition coSignFlow() {
    FlowDef flow = Flow.define("leaveCoSignV2", "请假流程会签 V2");
    flow.start("requestLeave");

    flow.node("requestLeave", BuiltInNodes.service("createLeaveRequest")
            .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
    flow.node("onLeave", BuiltInNodes.service("notifyManager"));
    flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
    flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("已拒绝"));

    flow.from("requestLeave").on(Outcomes.SUCCESS).to("approvals");
    flow.from("approvals").join(Dsl.all(
            Dsl.node("hrApproval", BuiltInNodes.approval("hr")),
            Dsl.node("managerApproval", BuiltInNodes.approval("manager"))
    )).on(Outcomes.APPROVED).to("onLeave")
      .on(Outcomes.REJECTED).to("rejected");
    flow.from("onLeave").on(Outcomes.SUCCESS).to("completed");

    return flow.build();
}

// 2. Worker 端注册业务 Activity
BusinessActivityRegistry act = new BusinessActivityRegistry();
act.register("createLeaveRequest", in -> Map.of("requestId", "REQ-" + UUID.randomUUID()));
act.register("notifyManager", in -> Map.of("notified", true));

NodeHandlerRegistry h = new NodeHandlerRegistry();
BuiltInHandlers.registerAll(h);
HandlerHolder.install(h);
WorkerSetup.setup(worker, h, act);

// 3. 客户端启动
GenericWorkflow stub = client.newWorkflowStub(GenericWorkflow.class, options);
WorkflowInput in = WorkflowInput.from(coSignFlow(), Map.of());
WorkflowClient.start(stub::execute, in);

// 4. 发 HR + manager 审批 signal（taskGroup 内的子任务用 child key 区分）
ObjectNode hrPayload = mapper.createObjectNode()
    .put(ApprovalTaskHandler.PAYLOAD_TASK_ID, "approvals/hrApproval")
    .put("decision", "approved");
stub.signal(new WorkflowEvent("approval", hrPayload));

ObjectNode mgrPayload = mapper.createObjectNode()
    .put(ApprovalTaskHandler.PAYLOAD_TASK_ID, "approvals/managerApproval")
    .put("decision", "approved");
stub.signal(new WorkflowEvent("approval", mgrPayload));

// 5. 等结果
WorkflowResult result = WorkflowStub.fromTyped(stub).getResult(WorkflowResult.class);
assert result.finalNodeId().equals("completed");
assert result.nodeResults().get("approvals").outcome().equals(Outcomes.APPROVED);
```

实际跑这个 demo 的 test：`LeaveTaskGroupExampleTest` ⭐

---

## 11. JSON 形态（配置层等价）

可视化 / 前端配置产出的 JSON 直接喂给 `Jackson` 反序列化为 `WorkflowDefinition`：

```json
{
  "id": "leaveCoSignV2",
  "name": "请假流程会签 V2",
  "startNodeId": "requestLeave",
  "nodes": [
    { "id": "requestLeave", "label": "requestLeave", "type": "serviceTask",
      "config": { "activity": "createLeaveRequest",
                  "activityInput": { "employeeId": "alice" } } },
    { "id": "approvals", "label": "approvals", "type": "taskGroup",
      "config": {
        "join": { "type": "all" },
        "tasks": [
          { "id": "hrApproval", "label": "hrApproval", "type": "approvalTask",
            "config": { "assignee": "hr" } },
          { "id": "managerApproval", "label": "managerApproval", "type": "approvalTask",
            "config": { "assignee": "manager" } }
        ]
      } },
    { "id": "onLeave", "label": "onLeave", "type": "serviceTask",
      "config": { "activity": "notifyManager" } },
    { "id": "completed", "label": "完成", "type": "endTask", "config": {} },
    { "id": "rejected", "label": "已拒绝", "type": "endTask", "config": {} }
  ],
  "edges": [
    { "from": "requestLeave", "to": "approvals", "event": "SUCCESS" },
    { "from": "approvals", "to": "onLeave", "event": "APPROVED" },
    { "from": "approvals", "to": "rejected", "event": "REJECTED" },
    { "from": "onLeave", "to": "completed", "event": "SUCCESS" }
  ]
}
```

字段 `event` / `outcome` / `when` 均可选；`@JsonInclude(NON_NULL)` 控制省略 null 值。

---

## 12. 测试覆盖

### 12.1 workflow-core

250+ 单元测试，覆盖：

- POJO 序列化往返（`DefinitionRoundtripTest` / `RecordValidationTest`）
- WorkflowGraph 可达性 + Tarjan 循环检测（`WorkflowGraphTest` / `GraphValidatorTest`）
- 9 个内置 handler 行为（`*HandlerTest`）
- DSL（`NodeBuilderTest` / `BuiltInNodesTest` / `CustomHandlerDslTest` / `EventEdgeTest` / `NestedJoinSpecTest`）
- ConditionEvaluator（旧自研语法 30 个 + Jakarta EL 7 个 = 37 个）
- DeterminismGuard（反射扫描 handler 字节码）
- Interceptor SPI 注册和契约

### 12.2 workflow-temporal local 端到端

`TestWorkflowEnvironment` 跑，无需远端 Temporal。17 通过 / 0 失败：

| 测试 | 场景 |
|------|------|
| `LeaveWorkflowTest` | 单审批 approved / rejected / timeout |
| `LeaveTaskGroupExampleTest` ⭐ | **会签 ALL**（HR + 经理都通过才放行） |
| `LeaveTaskGroupShortCircuitTest` | **会签 ANY** + 短路取消未完成的兄弟任务 |
| `LeaveBackEdgeWorkflowTest` | 回环 back-edge（驳回回起点重试） |
| `LeaveFlowDslDemoTest` | DSL demo |
| `ConditionRoutingTest` | `${largeAmount}` EL guard 分流 |
| `SagaCompensationTest` | Saga 反向补偿链 |
| `SubProcessTest` | 子工作流 |

跳过 3 个（需要远端 Temporal 或 Postgres）：`LeaveWorkflowRemoteTest` / `LeaveFlowDslRemoteTest` / `DatabaseArchiveIntegrationTest`。

---

## 13. 已知语义不一致 / 待清理

| # | 不一致 | 影响 | 解决方向 |
|---|--------|------|----------|
| 1 | ~~"等的事件名" 4 种拼法：`AWAIT_EVENT="awaitEvent"` vs `AWAITED_EVENT="awaitedEvent"` vs 字段不一致~~ | ✅ **已修（阶段 12）**：全部统一为 `awaitEvent`；`AWAITED_EVENT` 常量已删；`EventTaskConfig` 字段已重命名（保留 `@JsonAlias` 向后兼容） | — |
| 2 | ~~`NodeBuilder.set(k, v)` vs `.config(map)` vs `.config(POJO)`：名字不对称~~ | ✅ **已修（阶段 12）**：`config(Map)` → `setAll(Map)`；`config(Object)` → `setAll(Object)`；所有调用点已迁移 | — |
| 3 | `NodeResult.payload` / `NodeConfig.props` / `NodeDefinition.config(JsonNode)` 三个名字 | **非问题**：三者处于不同生命周期层（运行时产物 / DSL 中间态 / 持久化 JSON），名字巧合但语义不重叠，保留各自名字 | 不改代码 |
| 4 | ~~`BuiltInNodes.service(activity)` 无 `activityInput` 参数~~ | ✅ **已修（阶段 12）**：新增 `service(String, Map<String,Object>)` 双参重载 | — |
| 5 | `EdgeMatch.ByOutcome` 已 `@Deprecated`，`WorkflowGraph.nextCandidates` 内部仍引用 | ✅ **已修（阶段 12）**：在 `nextCandidates` 方法加 `@SuppressWarnings("deprecation")`；`GenericWorkflowImpl.pickNextEdge` legacy 分支同样 suppress；`ByOutcome` 本身保留以维持运行时向后兼容 | 下一个大版本可干净删除 |
| 6 | ~~Interceptor SPI 已建好但 `GenericWorkflowImpl` 未集成 hook 派发~~ | ✅ **已修（阶段 13/14）**：7/7 phase 全部接入。阶段 13 接 5 个（WORKFLOW_START / NODE_ENTER / NODE_EXIT / NODE_ERROR / WORKFLOW_END），阶段 14（F2）补 EVENT（`deliver` 内 `handler.onEvent` 之后）+ COMPENSATE（`runCompensation` 内 `handler.compensate` 之前）。`CompensationRecord` 加 `NodeDefinition` 字段；所有 `deliver` / `drive*` 链路传 `node` 参数 | — |
| 7 | ~~`ChildNodeSpec.nestedJoin` POJO 字段就绪但运行时不支持递归~~ | ✅ **已修（阶段 15）**：`FlowDef.buildTaskGroupConfig` 递归处理 `nestedJoin`——嵌套子节点序列化为 `type="taskGroup"` + recursively built config。Runtime 零改动（`executeChildSafely` → `registry.lookup("taskGroup")` → `TaskGroupHandler` → `driveTaskGroup` 链路天然递归） | — |
| 8 | ~~`flow.from(X).join(...)` 让 X 自变 taskGroup，X 的原配置静默丢失~~ | ✅ **已修（阶段 12）**：`FlowFrom.join()` 加前置校验：若 X 已注册为非 taskGroup 类型，立即抛 `IllegalStateException` + 清晰 message | — |

---

## 14. 不引入的依赖（设计决策）

| 依赖 | 不引入原因 |
|------|----------|
| Spring / Spring Expression Language (SpEL) | 不希望强制业务捆绑 Spring；Jakarta EL 是 JSR 标准 |
| `infrastructure-component-statemachine` | V1 历史选型，V3 直接基于 Temporal 自带状态机能力；引入会让模块依赖图复杂化 |
| `infrastructure-component-rule-engine` | rule-engine 的 `Condition<Facts>` 只解决"运行时执行"，不解决字符串表达式解析；用 Jakarta EL 一站解决 |
| `infrastructure-component-pdp` | PDP 是授权决策（subject/action/resource），跟流程条件评估不是同类问题 |
| MVEL / JEXL / OGNL / 其它 EL 实现 | Jakarta EL 是 JSR-341 标准，生态最稳；其他实现增加学习成本 |

如未来要重新评估，必须给出"Jakarta EL 不够用"或"已捆绑 Spring 栈反正都要带"的具体证据。

---

## 15. 实施情况

| 阶段 | 状态 |
|------|------|
| 阶段 1：核心定义模型 + JSON 序列化 | ✅ |
| 阶段 2：NodeHandler SPI + 9 个内置 handler | ✅ |
| 阶段 3：Java DSL（builder + BuiltInNodes + Flow chain + join） | ✅ |
| 阶段 4：WorkflowGraph 校验 + 可达性 + Tarjan 循环 | ✅ |
| 阶段 5：Jakarta EL ConditionEvaluator | ✅ |
| 阶段 6：Interceptor SPI（Deterministic + Async + Registry） | ✅ |
| 阶段 7：Temporal GenericWorkflow + RuntimeIntents 翻译 + Saga 补偿 + Signal + Timer + Activity + Child Workflow | ✅ |
| 阶段 8：端到端 demo（请假 + 会签 + 补偿 + 子流程 + 条件路由 + 回环） | ✅ 17 通过 / 0 失败 |
| 阶段 9：远程 Temporal + DB 归档集成测试 | ✅ 28 通过 / 0 失败（含 LeaveWorkflowRemoteTest / LeaveFlowDslRemoteTest / DatabaseArchiveIntegrationTest） |
| 阶段 10：V3 文档 | ✅ 本文档 |
| 阶段 11：DSL typed 自定义节点（`NodeType` 接口 + `BuiltInNodeType` enum + `NodeBuilder.setAll(POJO)`） | ✅ workflow-core 271 测试通过 |
| 阶段 12：§13 语义清理 + 死代码扫尾（E1 awaitEvent 统一 / E2 setAll 重命名 / E4 service 双参 / E5 deprecation suppress / E8 FlowFrom.join 校验） | ✅ workflow-core 276 测试通过；workflow-temporal compileTestJava 通过 |
| 阶段 13：Interceptor runtime 集成（F：5/7 phase 内联 + Activity 派发；新增 `InterceptorHolder` / `HookPhases` / `AsyncHookInvocation` / `AsyncInterceptorActivity(Impl)` / `SimpleInterceptorContext`；`WorkerSetup` 双 overload） | ✅ workflow-temporal 23 通过 / 0 失败；端到端 17 个 demo 不破 |
| 阶段 14：Interceptor EVENT + COMPENSATE（F2：`deliver` / `drive*` 链路传 node 参数；`CompensationRecord` 加 NodeDefinition；fireHook(EVENT) 在 onEvent 后；fireHook(COMPENSATE) 在 compensate 前） | ✅ workflow-temporal 27 通过 / 0 失败（+2 新 hook 测试） |
| 阶段 15：TaskGroup 递归 nestedJoin（G：`FlowDef.buildTaskGroupConfig` 递归子 taskGroup；runtime 零改动） | ✅ workflow-core 278 通过 + workflow-temporal 27 通过；2 层端到端 + 3 层 JSON 验证 |
| 阶段 16：归档去全局静态 + fire-and-forget（`WorkflowInput.archive` 5 号字段 → per-execution；`GenericWorkflowImpl` 删 `static archivalEnabled` / `enableArchival` / `isArchivalEnabled`；`archiveIfNeeded` 改 `Async.procedure` 入 `asyncHookPromises` 由 `awaitAsyncHooks` 吞错；删死代码 `WorkflowArchiverWorkflow(Impl)` / `InMemoryArchiveActivities` 及 `WorkerSetup` 中的注册） | ✅ 远端 `TEMPORAL_TARGET` 全模块 28 通过 / 0 失败（修复 27 failed 全局污染问题） |
| 阶段 17：Signal 事件幂等（`WorkflowEvent` 3 分量 + `eventId` 可空幂等键；保留 2 参兼容构造器 + `@JsonInclude(NON_NULL)` 向后兼容；`GenericWorkflowImpl` 加 `processedEventIds: Set<String>`，`signal()` 入队前去重；null/blank eventId 不参与去重；确定性 replay 安全；堵住客户端重发 + 回环误消费两类 gap） | ✅ |

---

## 16. 后续工作（按优先级）

1. **下一个大版本可彻底删除 `EdgeMatch.ByOutcome`**——目前 deprecated + suppress；删除前需保证所有手构 `EdgeDefinition` 都已迁移到 `event` 字段

---

## 17. 文件参考

| 关注什么 | 起点 |
|---------|------|
| POJO 形状 / JSON 形态 | `workflow-core/.../definition/*.java` |
| SPI 抽象 | `workflow-core/.../spi/*.java` |
| 自定义 handler 怎么写 | 参考 `workflow-core/.../handlers/ServiceTaskHandler.java` |
| 自定义 handler 怎么注册 | `workflow-core/.../spi/NodeHandlerRegistry.java` |
| 自定义 NodeType / typed config | `workflow-core/.../spi/NodeType.java` + `BuiltInNodeType.java` + `test/.../dsl/CustomHandlerTypedDslTest.java` |
| DSL 怎么用 | 本文档 §7 + `workflow-temporal/test/.../leave/LeaveProcess.java` |
| 表达式语法 | `workflow-core/.../spi/ConditionEvaluator.java` 的 class javadoc |
| Temporal 适配 | `workflow-temporal/.../runtime/GenericWorkflowImpl.java` |
| Interceptor 集成 | `workflow-temporal/.../runtime/InterceptorHolder.java` + `HookPhases.java` + `AsyncInterceptorActivity(Impl).java` + `test/.../interceptor/InterceptorIntegrationTest.java` |
| 端到端 demo | `workflow-temporal/test/.../leave/LeaveTaskGroupExampleTest.java` |
