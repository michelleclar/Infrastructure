# infrastructure-component-workflow-temporal

> 基于 Temporal 的配置驱动工作流引擎。把流程图（节点 + 路由 + 信号 + 补偿 + 嵌套会签）写成可序列化的 `WorkflowDefinition`，由通用 `GenericWorkflowImpl` 解释执行——业务方只写**节点配置**和**业务 Activity**，不写 Temporal 工作流代码。

## 模块拆分

```
infrastructure-component-workflow-core         纯 Java，无 Temporal 依赖
  definition/  WorkflowDefinition / NodeDefinition / EdgeDefinition / NodeResult / NodeStatus
  dsl/         Flow.define(...) FlowDef / NodeBuilder / BuiltInNodes / JoinSpec
  handlers/    9 个 BuiltInHandler（serviceTask / approvalTask / userTask / eventTask
               timerTask / taskGroup / gateway / subProcess / endTask）
  spi/         NodeHandler<C> 接口 / NodeHandlerRegistry / NodeType / NodeTypes
  graph/       WorkflowGraph + GraphValidator（可达性 + Tarjan 循环）
  interceptor/ DeterministicInterceptor / AsyncInterceptor / WorkflowInterceptorRegistry

infrastructure-component-workflow-temporal      Temporal 运行时适配
  runtime/  GenericWorkflowImpl / WorkerSetup / WorkflowInput / WorkflowResult
            BusinessActivityRegistry / HandlerHolder / InterceptorHolder
  archive/  ArchiveActivities / DatabaseArchiveActivities（可选）
  example/  QuickStartExample / QuickStartWorker / QuickStartClient
```

只写流程定义/想要 headless 校验：依赖 `workflow-core`。  
要在 Temporal 上跑起来：依赖 `workflow-temporal`（传递引入 `workflow-core`）。

---

## 依赖

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.carl:infrastructure-component-workflow-temporal:1.0-BATE")
}
```

强制要求：
- **JDK 21**
- **Temporal Server** —— 远端连 `setTarget("host:port")`；本地起 `temporalio/temporal:latest` docker
- 归档可选：PostgreSQL（用 `DatabaseArchiveActivities`）或自实现 `ArchiveActivities`

---

## 推荐入口：`WorkflowEngine` 门面（业务代码 0 import `io.temporal`）

如果你只想「跑起来」，用 `WorkflowEngine` 门面 —— 它把 service stubs / client / worker / typed stub 全封进去，业务代码不碰任何 `io.temporal.*`：

```java
// 只 import 引擎的 runtime 包 + core，0 import io.temporal.*
NodeHandlerRegistry handlers = new NodeHandlerRegistry();
BuiltInHandlers.registerAll(handlers);
BusinessActivityRegistry activities = new BusinessActivityRegistry();
activities.register("createLeaveRequest", in -> Map.<String,Object>of("requestId", "REQ-1"));
activities.register("notifyManager", in -> Map.<String,Object>of("notified", true));

try (WorkflowEngine engine =
        WorkflowEngine.connect(EngineConfig.of("localhost:7233", "MY_QUEUE"))
                .withWorker(handlers, activities)) {           // 起 worker

    WorkflowHandle h = engine.start(definition, Map.of("employee", "alice"));  // 发起
    h.signal("approval", approvedPayload);                     // 发审批 signal（payload 是 Jackson JsonNode）
    WorkflowResult result = h.awaitResult();                  // 等结果
    System.out.println(result.finalStatus() + " @ " + result.finalNodeId());
}
```

- `EngineConfig(target, namespace, taskQueue)` 用纯 String 配置；`EngineConfig.of(target, queue)` 用 `default` namespace。
- `withWorker(handlers, activities[, archive][, interceptors])` 注册并启动 worker。
- `WorkflowHandle`：`signal(name, payload[, eventId])` / `query()` / `awaitResult()`；`engine.attach(workflowId)` 可附着到已运行的实例。
- `WorkflowEngine` 是 `AutoCloseable`，try-with-resources 自动关 worker + 连接。

> Temporal 仍是**运行时依赖**（jar 在 classpath、server 要起）；门面只是隐藏 API 表面，让业务代码编译期 0 碰 `io.temporal.*`。下面的「手动」写法展示门面底层做了什么。

---

## 最小可工作示例（手动 worker/client，底层写法）

业务：员工请假，主管审批，通过则通知。

### 1. 启动 Worker

```java
public class MyWorker {
    public static void main(String[] args) {
        var service = WorkflowServiceStubs.newInstance(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget("localhost:7233")
                        .build());
        var factory = WorkerFactory.newInstance(WorkflowClient.newInstance(service));
        var worker = factory.newWorker("MY_QUEUE");

        // 1. 注册内置节点 handler
        var handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);

        // 2. 注册业务 Activity（按名字）
        var activityRegistry = new BusinessActivityRegistry();
        activityRegistry.register("createLeaveRequest", input -> {
            String employeeId = (String) input.get("employeeId");
            return Map.of("requestId", "REQ-" + UUID.randomUUID(), "employeeId", employeeId);
        });
        activityRegistry.register("notifyManager", input -> {
            System.out.println("通知主管: " + input);
            return Map.of("notified", true);
        });

        // 3. 绑定到 Worker
        WorkerSetup.setup(worker, handlerRegistry, activityRegistry);
        factory.start();
        System.out.println("Worker 启动，监听 MY_QUEUE");
    }
}
```

### 2. 客户端：定义流程 + 启动

```java
public class MyClient {
    public static void main(String[] args) throws Exception {
        // ── 定义流程 ─────────────────────────────────────────
        FlowDef flow = Flow.define("leave", "请假流程");
        flow.start("requestLeave");

        flow.node("requestLeave", BuiltInNodes.service("createLeaveRequest")
                .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node("approval", BuiltInNodes.approval("bob"));
        flow.node("notify", BuiltInNodes.service("notifyManager"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("拒绝"));

        flow.from("requestLeave").on("SUCCESS").to("approval");
        flow.from("approval").on("APPROVED").to("notify");
        flow.from("approval").on("REJECTED").to("rejected");
        flow.from("notify").on("SUCCESS").to("completed");

        WorkflowDefinition def = flow.build();

        // ── 启动 ─────────────────────────────────────────────
        var service = WorkflowServiceStubs.newInstance(
                WorkflowServiceStubsOptions.newBuilder().setTarget("localhost:7233").build());
        var client = WorkflowClient.newInstance(service);

        var stub = client.newWorkflowStub(GenericWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("MY_QUEUE")
                        .setWorkflowId("leave-" + UUID.randomUUID())
                        .build());

        WorkflowInput input = WorkflowInput.from(def,
                Map.of("employee", "alice", "manager", "bob", "days", 3));
        // 业务数据通过 NodeExecutionContext.businessData() 暴露给 handler；
        // Service Task 的 activityInput 是字面 Map，没有占位符替换——需要动态值
        // 时在 Handler 内自己从 businessData/variables 拼装。

        WorkflowClient.start(stub::execute, input);

        // ── 等到审批节点后发 signal ───────────────────────────
        Thread.sleep(2000);
        var payload = new ObjectMapper().createObjectNode().put("decision", "approved");
        stub.signal(new WorkflowEvent("approval", payload));

        WorkflowResult result = WorkflowStub.fromTyped(stub).getResult(WorkflowResult.class);
        System.out.println("finalStatus = " + result.finalStatus());
        System.out.println("finalNodeId = " + result.finalNodeId());
    }
}
```

> 完整可跑的示例：`src/test/java/.../example/QuickStartWorker.java` 和 `QuickStartClient.java`。

---

## 核心概念速查

### 内置节点类型（`NodeTypes`）

| 常量 | DSL 工厂 | 等待语义 |
|---|---|---|
| `SERVICE_TASK` | `BuiltInNodes.service("activityName")` | 同步调用业务 Activity |
| `APPROVAL_TASK` | `BuiltInNodes.approval("assignee")` | 等审批 signal（默认 `approval`），出 `APPROVED`/`REJECTED`/`TIMEOUT` |
| `USER_TASK` | `BuiltInNodes.userTask("assignee")` | 等用户提交 signal |
| `EVENT_TASK` | `BuiltInNodes.event("signalName")` | 等指定 signal |
| `TIMER_TASK` | `BuiltInNodes.timer("PT1H")` | `Workflow.sleep` |
| `TASK_GROUP` | `BuiltInNodes.taskGroup()` | 并行子节点 + `JoinRule(all/any)`，支持嵌套 |
| `GATEWAY` | `BuiltInNodes.gateway()` | 纯路由分叉 |
| `SUB_PROCESS` | `BuiltInNodes.subProcess("subFlowId")` | child workflow |
| `END_TASK` | `BuiltInNodes.endTask()` | 终止节点 |

### 标准路由值

`SUCCESS` / `FAILED` / `APPROVED` / `REJECTED` / `SENDBACK` / `TIMEOUT` / `RECEIVED` / `TRIGGERED` / `CANCELLED` / `COMPLETED`

### 路由（`flow.from(X)` 链式）

```java
flow.from("approval").on("APPROVED").to("notify");
flow.from("approval").on("REJECTED").to("rejected");

// 条件路由：同一 event 多条边，按声明顺序匹配 .when 表达式；最后一条不带 .when
// 作为 fall-through（默认）。表达式是 Jakarta EL，必须 ${...} 包裹。
flow.from("requestLeave").on("SUCCESS")
    .when("${largeAmount}").to("bigApproval");
flow.from("requestLeave").on("SUCCESS").to("normalApproval");  // 默认分支
```

EL 上下文可读（详见 `ConditionEvaluator`）：
- `${varName}` — 裸标识符，从 `ctx.variables()` 取（初始值来自 `WorkflowInput.initialVariables`，handler 可改写）
- `${variables.x}` / `${businessData.a.b}` — Map / JsonNode 嵌套访问
- `${results['nodeId'].outcome}` / `${results.nodeId.outcome}` — 之前已完成节点的 `NodeResult`
- `${ctx.variables}` / `${ctx.businessData}` / `${ctx.results}` — 聚合 view
- 顶层 `true` / `false`（不需 `${}`）作字面量直接走


### 会签 / 或签（`taskGroup` + join）

```java
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

flow.from("submit").on("SUCCESS").to("approvals");
flow.from("approvals")
    .join(all(                    // 全签：all 全过才 APPROVED；any 用 any(...)
        node("hr", BuiltInNodes.approval("hr")),
        node("manager", BuiltInNodes.approval("manager"))))
    .on("APPROVED").to("notify")
    .on("REJECTED").to("rejected");

// 嵌套：外层 ALL，内层一组用 ANY
JoinSpec mgmt = any(
    node("manager", BuiltInNodes.approval("manager")),
    node("cfo",     BuiltInNodes.approval("cfo")));
flow.from("approvals")
    .join(all(
        node("hr", BuiltInNodes.approval("hr")),
        node("mgmt", new NodeConfig(NodeTypes.TASK_GROUP, Map.of()), mgmt)))
    .on("APPROVED").to("notify")
    .on("REJECTED").to("rejected");
```

### Signal 收发

```java
// 发：name 对应 EventTask / ApprovalTask 等待的事件名（默认 "approval"）
stub.signal(new WorkflowEvent("approval",
        mapper.createObjectNode().put("decision", "approved")));

// 收：handler 内 onEvent 收到 WorkflowEvent，对 ApprovalTask 检查 payload.decision
```

---

## 业务 Activity

`BusinessActivityRegistry` 按名字注册 `Function<Map<String, Object>, Map<String, Object>>`：

```java
activityRegistry.register("calculatePrice", input -> {
    int qty = (int) input.get("qty");
    double unit = (double) input.get("unitPrice");
    return Map.of("total", qty * unit);
});
```

在节点里通过 `activityInput` 字段传参——这是一个**字面 Map**，会原样传给业务 activity（`ServiceTaskHandler` 不做占位符替换）：

```java
flow.node("calc", BuiltInNodes.service("calculatePrice")
        .andThen(b -> b.set("activityInput", Map.of(
                "qty", 10,
                "unitPrice", 99.5))));
```

需要把 `WorkflowInput.businessData` / 前序节点 result 里的值拼进 input 时，目前两种选择：
1. **客户端构造时**就把动态值算好塞进 definition（最简单，但失去复用性）
2. **写自定义 NodeHandler** 包一层 `serviceTask`：在 handler 的 `run` 里读 `ctx.businessData()` / `ctx.resultOf(...)` 拼 `activityInput`，再返回 WAITING + ACTIVITY intent（保持 definition 通用）

---

## 自定义节点类型

`NodeHandler` 的 `run` / `onEvent` / `compensate` 都跑在 **Temporal 工作流线程**，必须是**纯确定性函数**：禁止 I/O、时钟、随机、阻塞、可变共享状态。需要副作用的事都得通过 `RuntimeIntents.ACTIVITY` 让 runtime 派发到 Activity。

```java
public record SmsConfig(String template, String to) {}

public class SmsHandler implements NodeHandler<SmsConfig> {

    @Override public String type() { return "sms"; }

    @Override public Class<SmsConfig> configType() { return SmsConfig.class; }

    @Override
    public NodeResult run(NodeExecutionContext ctx, SmsConfig config) {
        // ✅ 返回 WAITING + ACTIVITY intent，让 runtime 调注册过的业务 activity
        Map<String, Object> payload = Map.of(
                RuntimeIntents.ACTIVITY, "sendSms",
                RuntimeIntents.ACTIVITY_INPUT, Map.of(
                        "to", config.to(),
                        "template", config.template()));
        return new NodeResult(NodeStatus.WAITING, null, payload, null);
        // ❌ 不要直接 smsClient.send(...)，违反确定性合约会让 Temporal replay 崩
    }

    @Override
    public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, SmsConfig config) {
        return event != null && "_activityResult".equals(event.name());
    }

    @Override
    public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, SmsConfig config) {
        // runtime 把 activity 结果合成 "_activityResult" 事件回灌
        return NodeResult.completed("SUCCESS");
    }
}

// Worker 启动时
handlerRegistry.register(new SmsHandler());                       // 用户 handler 走 DeterminismGuard 静态检查
activityRegistry.register("sendSms", input -> {                   // 配套业务 activity
    smsClient.send((String) input.get("to"), (String) input.get("template"));
    return Map.of("sent", true);
});
```

> **`register` vs `registerBuiltIn`**：`register(...)` 会调 `DeterminismGuard.assertPure(handlerClass)` 做静态字节码检查（拦明显的 I/O / 时钟 / 随机调用）；只有库内置 handler 用 `registerBuiltIn(...)` 跳过。

DSL 引用：

```java
// 写法 A：字符串 type
flow.node("notify", b -> b.type("sms").set("template", "你好").set("to", "13800000000"));

// 写法 B：类型化（推荐，IDE 补全 + POJO 一次性塞进去）
public enum MyNodeType implements NodeType {
    SMS;
    @Override public String value() { return "sms"; }
}
flow.node("notify", b -> b.type(MyNodeType.SMS).setAll(new SmsConfig("你好", "13800000000")));
```

---

## Interceptor（生命周期 Hook）

7 个 phase：`WORKFLOW_START` / `NODE_ENTER` / `NODE_EXIT` / `NODE_ERROR` / `EVENT` / `COMPENSATE` / `WORKFLOW_END`。

两种 SPI：
- **DeterministicInterceptor**——纯函数，工作流线程内联执行，必须确定（适合 metrics 计数器、变量记录）
- **AsyncInterceptor**——通过专用 Activity 派发，可做 I/O（适合写 Kafka、HTTP 推送）

```java
var registry = new WorkflowInterceptorRegistry();

// 内联：覆盖你关心的方法即可，其他保留 default no-op
registry.register(new DeterministicInterceptor() {
    @Override
    public void onNodeEnter(InterceptorContext ctx, NodeDefinition node) {
        // 工作流线程内做轻量记账（必须确定性，禁 I/O / 时钟 / 随机）
    }
});

registry.register(new AsyncInterceptor() {
    @Override
    public void onWorkflowEnd(InterceptorContext ctx, NodeResult terminalResult) {
        kafkaTemplate.send("workflow-events", terminalResult.outcome());
    }
});

WorkerSetup.setup(worker, handlerRegistry, activityRegistry, registry);  // 带 interceptor 的 overload
```

`DeterministicInterceptor` / `AsyncInterceptor` 都暴露 7 个 default 方法：`onWorkflowStart` / `onWorkflowEnd` / `onNodeEnter` / `onNodeExit` / `onNodeError` / `onEvent` / `onCompensate`。

异步 hook 失败不会影响主流程（`awaitAsyncHooks` 统一吞错）。

---

## 归档（可选）

写 PostgreSQL：

```java
// Worker 端：注册归档 Activity
DatabaseArchiveActivities archive = new DatabaseArchiveActivities(DB_URL, DB_USER, DB_PASSWORD);
WorkerSetup.setup(worker, handlerRegistry, activityRegistry, archive);

// Client 端：per-execution 显式打开
WorkflowInput input = WorkflowInput.from(def, businessData).withArchive(true);
```

**关键点**：归档开关在 `WorkflowInput`（per-execution），不再是 JVM-wide static。同一个 Worker 可以有些流程归档、有些不归档。归档失败走 `Async.procedure` fire-and-forget，**不会拖垮主流程**。

自实现归档：`implements ArchiveActivities { void archive(WorkflowInstanceSnapshot) }`。

---

## 测试

```bash
# 本地（用 Temporal TestWorkflowEnvironment）
./gradlew :infrastructure-component-workflow-temporal:test

# 远端真实 Temporal + PG（远程集成测试）
TEMPORAL_TARGET=180.184.66.147:31733 \
./gradlew :infrastructure-component-workflow-temporal:test
```

写自己的集成测试可以参考：
- `LeaveWorkflowTest.java`——单审批：approved / rejected / timeout 三路
- `LeaveTaskGroupExampleTest.java`——会签 + taskId 匹配
- `LeaveTaskGroupShortCircuitTest.java`——或签 ANY 短路
- `LeaveNestedTaskGroupTest.java`——嵌套会签（外层 ALL + 内层 ANY）
- `LeaveBackEdgeWorkflowTest.java`——驳回回环
- `SubProcessTest.java`——子工作流
- `SagaCompensationTest.java`——saga 补偿
- `ConditionRoutingTest.java`——Jakarta EL 条件路由
- `InterceptorIntegrationTest.java`——7 phase 全验证

---

## 排错速查

| 现象 | 原因 / 处理 |
|---|---|
| `NodeHandlerRegistry not installed` | 没调 `WorkerSetup.setup`；或者在 workflow 代码里直接 `new` 了 Registry |
| `Activity Type "Archive" is not registered` | Worker 没注册 archive activity，但 `WorkflowInput.archive=true` → 关闭归档或注册 `DatabaseArchiveActivities` |
| `Activity Type "Execute" is not registered` | Worker 漏了 `WorkerSetup.setup(...)`（它注册 `GenericActivity` 实现，Temporal 把方法名 `execute` 翻成活动类型 `Execute`） |
| Signal 报 `NOT_FOUND: workflow execution already completed`（且 workflow 在 ApprovalTask 之前就完了） | `.on("approved")` 等 event 名用了**小写字面值**，但 `"SUCCESS"`/`"APPROVED"` 都是**大写**，`pickNextEdge` 大小写敏感匹配不上 → 路由失败，workflow 在当前节点终止。统一用 handler 实际返回的大写路由值 |
| Service Task 跑起来抛 "no activity registered for ..." | `BusinessActivityRegistry` 找不到对应业务 activity 名字 → 检查 `register(name, lambda)` 拼写 |
| Workflow 卡在 ApprovalTask | Signal 名字不对，默认是 `approval`；可在节点里 `.set("awaitEvent", "yourName")` 改 |
| `WorkflowDefinition` 校验失败 | `GraphValidator` 报告：节点不可达 / 有环 / start node 无入度等；按提示修拓扑 |
| 远端 Temporal 跑测试个别失败 | 检查归档 flag 是不是 per-execution；旧版 static 已废，所有 caller 应该走 `WorkflowInput.archive` |

---

## 设计文档

完整设计：[`docs/workflow-v3-design.md`](../docs/workflow-v3-design.md)

包含 17 节：领域模型 / SPI / DSL / 路由 / WAITING 翻译 / 会签嵌套 / Interceptor / 归档 / 实施阶段 / 后续工作。

---

## License

跟随项目主 LICENSE。
