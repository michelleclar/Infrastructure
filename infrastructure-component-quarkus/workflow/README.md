# infrastructure-component-quarkus / workflow

把通用 Temporal 工作流引擎（`infrastructure-component-workflow-temporal`）接入 Quarkus CDI 应用。
启动时自动从容器收集节点 handler / 业务 activity / 归档 / 拦截器，拉起**一个** Temporal worker，
并暴露 `WorkflowFacade` 供业务发起、发信号、查询、等待结果——业务代码只碰 POJO / `WorkflowDefinition`，
不写任何 Temporal 工作流代码。

> 引擎本身的概念（Flow DSL、节点类型、路由、会签、归档、Interceptor、确定性合约）见
> [`infrastructure-component-workflow-temporal/README.md`](../../infrastructure-component-workflow-temporal/README.md)。
> 本模块只讲 **CDI 接线**。

---

## 依赖

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:workflow"))
}
```

本模块通过 `api` 传递引入 `workflow-temporal` + `workflow-core` + quarkiverse `quarkus-temporal`，
所以业务侧能直接用 `WorkflowDefinition` / `Flow` DSL / `WorkflowEvent` 等类型。

`io.temporal.client.WorkflowClient` 这个 CDI bean 由 quarkus-temporal 扩展产出，连接参数走它的配置
（target / namespace 等，见 quarkiverse-temporal 文档）。本模块**复用**该 client，不自己建连接。

---

## 配置（前缀 `workflow`）

| key | 默认值 | 说明 |
|-----|--------|------|
| `workflow.task-queue` | `workflow-tasks` | worker 轮询、发起工作流时使用的 Temporal task queue |
| `workflow.start-worker` | `true` | `false` = 仅客户端（发起/信号/查询），不在本进程起 worker |

```properties
workflow.task-queue=leave-tasks
workflow.start-worker=true
```

`workflow.start-worker=false` 的典型场景：API 节点只负责发起/驱动工作流，真正执行的 worker 部署在别的进程。
此时 `WorkflowFacade` 照常可用。

---

## 你要提供的 CDI bean

`WorkflowBootstrap` 在 `StartupEvent` 时按类型收集下列 bean 装配 worker。除节点 handler 外都是可选，
**每种至多提供一个**：

| 贡献点 | 形式 | 必需 | 说明 |
|--------|------|------|------|
| 自定义节点 handler | 实现 `NodeHandler` 的 `@Singleton` bean（可多个） | 否 | 内置 handler 始终注册；你的 handler 追加注册，会过 `DeterminismGuard` 静态检查 |
| 业务 activity | 产出一个 `BusinessActivityRegistry` 的 `@Produces` 方法 | 否* | service task 调用的业务函数；不提供则为空注册表 |
| 归档 | 一个 `ArchiveActivities` bean | 否 | 提供则按 `WorkflowInput.archive` per-execution 开关归档 |
| 拦截器 | 一个 `WorkflowInterceptorRegistry` bean | 否 | 7 phase 生命周期 hook |
| JSON | 应用的 `ObjectMapper`（Quarkus 默认就有） | 自动 | 启动时 install 进 `ObjectMapperHolder`，让引擎序列化与应用 Jackson 配置一致 |

> \* 流程里只要有 service task，就需要对应的业务 activity，否则运行时报 “no activity registered”。

> **为什么 handler 要 `@Singleton`**：`@ApplicationScoped` 会被 Quarkus 包一层 client proxy，
> `DeterminismGuard` 扫到的是代理类而非真实类，既绕过了纯度检查、又在工作流线程上多一层间接。
> `@Singleton`（或 `@Dependent`）是真实实例。

### 示例：贡献 handler + activity

```java
// 1) 自定义节点 handler —— 必须 @Singleton，且满足确定性合约（禁 I/O / 时钟 / 随机）
@Singleton
public class SmsHandler implements NodeHandler<SmsConfig, Void, Void> {
    @Override public String type() { return "sms"; }
    @Override public Class<SmsConfig> configType() { return SmsConfig.class; }
    @Override public NodeResult run(NodeExecutionContext ctx, SmsConfig cfg) {
        // 副作用通过 ACTIVITY intent 派发，不在此直接发短信
        return new NodeResult(NodeStatus.WAITING, null,
                Map.of(RuntimeIntents.ACTIVITY, "sendSms",
                       RuntimeIntents.ACTIVITY_INPUT, Map.of("to", cfg.to())), null);
    }
    /* canAccept / onEvent 省略，见 temporal README */
}

// 2) 业务 activity —— 产出一个 BusinessActivityRegistry
@ApplicationScoped
public class WorkflowActivities {
    @Produces @ApplicationScoped
    public BusinessActivityRegistry activities(SmsClient sms) {
        BusinessActivityRegistry reg = new BusinessActivityRegistry();
        reg.register("createLeaveRequest", in -> Map.of("requestId", "REQ-1"));
        reg.register("sendSms", in -> { sms.send((String) in.get("to")); return Map.of("sent", true); });
        return reg;
    }
}
```

---

## 用 `WorkflowFacade` 驱动流程

`WorkflowFacade` 是 `@ApplicationScoped` bean，注入即用。`workflowId` 是你这条实例的幂等键
（如 `"leave-" + bizId`），后续 signal/query/await 用同一个 id 寻址。

```java
@ApplicationScoped
public class LeaveService {

    @Inject WorkflowFacade workflow;

    public void apply(String bizId) {
        WorkflowDefinition def = buildLeaveFlow();          // Flow DSL 构造，见 temporal README
        workflow.start(def, "leave-" + bizId,
                Map.of("employee", "alice", "days", 3));    // businessData
    }

    public void approve(String bizId, boolean ok) {
        JsonNode payload = JsonNodeFactory.instance.objectNode()
                .put("decision", ok ? "approved" : "rejected");
        workflow.signal("leave-" + bizId, "approval", payload);
    }

    public WorkflowState peek(String bizId) {
        return workflow.query("leave-" + bizId);
    }

    public WorkflowResult await(String bizId) {            // 阻塞直到终态
        return workflow.awaitResult("leave-" + bizId);
    }
}
```

`WorkflowFacade` 方法一览：

| 方法 | 作用 |
|------|------|
| `start(def, workflowId, businessData)` / `start(..., archive)` | 发起实例（fire-and-forget），返回 workflowId |
| `signal(workflowId, eventName, payload)` | 发外部事件（如审批决定）；`eventName` 是目标节点等待的事件名（如 `"approval"`） |
| `signal(workflowId, eventName, payload, eventId)` | 带幂等键的信号，runtime 按 `eventId` 去重 |
| `query(workflowId)` → `WorkflowState` | 非阻塞读当前状态 |
| `awaitResult(workflowId)` → `WorkflowResult` | 阻塞等终态结果 |

facade 全程用引擎的**类型化** `GenericWorkflow` stub，不靠字符串方法名寻址 signal/query。

---

## 启停语义

- `@Observes StartupEvent`：装配 worker（除非 `start-worker=false`）并 `factory.start()`。
- `@PreDestroy`：`factory.shutdown()` 关闭本进程的 worker。

---

## 日志

```properties
quarkus.log.category."org.carl.infrastructure.workflow".level=DEBUG
```

前缀同时覆盖引擎（`workflow-core` / `workflow-temporal`）与本适配层（`...workflow.quarkus`）。
facade 的 query/await 走 `TRACE`，把上面的 `DEBUG` 换成 `TRACE` 可看到。
