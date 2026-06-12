# Workflow V2 Design Draft

## 背景

当前 `infrastructure-component-workflow-temporal` 更接近“事件驱动状态机 + Temporal 持久化”：

```java
flow.from(A).to(B).on(EVENT).perform(activity);
flow.approval(APPROVAL_STATE).expr(...).onApproved(...).onRejected(...);
```

这个模型能表达状态流转，但在审批、并行、会签、子任务、页面可视化配置等场景下容易把概念混在一起：

- `approval(state)` 把审批提升成主流程状态级 API。
- `perform(activity)` 绑定在 transition 上，容易被理解成“节点业务逻辑”。
- 普通节点没有“执行成功自动进入下一节点”的语义。
- 流程定义和运行时类型绑定较重，不利于 JSON 序列化和页面配置。

V2 目标是把工作流重新建模为“可视化流程图 + 可插拔节点 + 运行时结果路由”。

## 设计原则

1. 流程图负责路由，节点负责执行并产出结果。
2. 审批不是特殊主流程 API，而是一个可注册的节点类型。
3. DSL 用于“画流程”，最终编译成统一的 `WorkflowDefinition`。
4. 页面配置和 Java DSL 都应生成同一种 JSON 化定义。
5. Temporal 只作为 runtime adapter，不进入核心模型 API。
6. 节点定义和节点运行状态必须分离。
7. 节点 outcome 和下一节点状态必须分离。

## 核心概念

### WorkflowDefinition

完整流程定义，可序列化、可存储、可由页面配置或 Java DSL 生成。

```java
public record WorkflowDefinition(
        String id,
        String name,
        List<NodeDefinition> nodes,
        List<EdgeDefinition> edges
) {}
```

### NodeDefinition

流程图中的一个节点实例。

```java
public record NodeDefinition(
        String id,
        String label,
        String type,
        String templateId,
        JsonNode config
) {}
```

字段语义：

- `id`：流程内唯一稳定标识，用于连线、条件引用、运行时记录。
- `label`：页面显示名，可以重复，可以修改。
- `type`：节点类型，例如 `serviceTask`、`approvalTask`、`taskGroup`。
- `templateId`：可选，引用页面中的节点模板。
- `config`：节点配置，由对应 `NodeHandler` 解释。

不要使用中文 `label` 作为流程内部 key。

### EdgeDefinition

节点之间的连线。

```java
public record EdgeDefinition(
        String from,
        String to,
        String event,
        String outcome,
        String condition
) {}
```

字段语义：

- `from`：源节点 id。
- `to`：目标节点 id。
- `event`：可选，外部事件名称。
- `outcome`：可选，源节点完成后的结果。
- `condition`：可选，条件表达式。

### NodeHandler

节点类型插件。核心引擎只认识 `type`，具体执行逻辑由 handler 提供。

```java
public interface NodeHandler<C> {

    String type();

    Class<C> configType();

    NodeResult run(NodeExecutionContext ctx, C config);

    default boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, C config) {
        return false;
    }

    default NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, C config) {
        return NodeResult.waiting();
    }

    default boolean compensable() {
        return false;
    }

    default void compensate(NodeExecutionContext ctx, C config, NodeResult result) {}
}
```

`C` 只表示某种节点的配置类型，不进入 `WorkflowDefinition`。定义层保留 `JsonNode config`，运行时根据 `configType()` 转换。

### NodeResult

节点执行结果。

```java
public record NodeResult(
        NodeStatus status,
        String outcome,
        Map<String, Object> payload,
        String message
) {
    public static NodeResult waiting() {
        return new NodeResult(NodeStatus.WAITING, null, Map.of(), null);
    }

    public static NodeResult completed(String outcome) {
        return new NodeResult(NodeStatus.COMPLETED, outcome, Map.of(), null);
    }

    public static NodeResult failed(String message) {
        return new NodeResult(NodeStatus.FAILED, "FAILED", Map.of(), message);
    }
}
```

```java
public enum NodeStatus {
    WAITING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

`outcome` 是节点输出契约，用于流程路由。例如：

- `approvalTask`：`APPROVED`、`REJECTED`、`SENDBACK`、`TIMEOUT`
- `serviceTask`：`SUCCESS`、`FAILED`
- `eventTask`：`RECEIVED`、`TIMEOUT`
- `timerTask`：`TRIGGERED`、`CANCELLED`
- `subProcess`：`COMPLETED`、`CANCELLED`、`FAILED`

### WorkflowEvent

外部事件。

```java
public record WorkflowEvent(
        String name,
        JsonNode payload
) {}
```

不强制使用 Java enum，便于页面配置、远程回调和 JSON 序列化。Java DSL 可以提供 enum 语法糖。

### NodeExecutionContext

节点运行上下文。

```java
public interface NodeExecutionContext {

    String workflowId();

    String instanceId();

    String currentNodeId();

    JsonNode businessData();

    Map<String, Object> variables();

    NodeResult resultOf(String nodeId);

    WorkflowEvent currentEvent();
}
```

上下文必须是一等公民。节点需要通过它读取业务数据、流程变量、历史节点结果和当前事件。

## 内置节点类型

V2 先内置以下节点类型，后续允许业务侧注册更多类型。

### serviceTask

进入节点后执行业务代码。

结果：

- 成功：`SUCCESS`
- 异常：`FAILED`

用途：

- 写库
- 调用外部接口
- 发消息
- 生成业务单据

### approvalTask

创建审批待办，等待审批事件。

结果：

- `APPROVED`
- `REJECTED`
- `SENDBACK`
- `TIMEOUT`

审批节点不直接修改主流程状态，只产出 outcome。打回到哪里由流程边决定。

### userTask

通用人工任务。

结果：

- `COMPLETED`
- `CANCELLED`
- `TIMEOUT`

审批可以实现为特殊的 `userTask`，但为了常见业务体验，可作为内置节点类型保留。

### eventTask

等待外部事件。

结果：

- `RECEIVED`
- `TIMEOUT`

### timerTask

等待时间。

结果：

- `TRIGGERED`
- `CANCELLED`

### taskGroup

创建一组子任务，并根据 join 规则聚合子任务结果。

典型场景：

- 并行审批
- 会签
- 或签
- 多个异步任务全部完成后继续
- 任意一个任务成功后继续

`taskGroup` 是节点类型，不是流程边上的特殊语法。

### gateway

不执行外部业务，只根据条件立即产出某个 outcome。

### subProcess

启动子流程，子流程结束后把子流程结果映射为当前节点 outcome。

### endTask

流程结束节点。

## DSL 方向

外部 DSL 应尽量表达流程意图，不强迫用户每次显式写内部 outcome 表达式。

### 简洁写法

```java
flow.from("发起请假")
        .on("提交")
        .to("发起请假审批");

flow.from("发起请假审批")
        .join(all(
                node("HR 审批", approval(...)),
                node("部门主管审批", approval(...))
        ))
        .on("审批通过").to("休假")
        .on("审批拒绝").to("发起请假");
```

这类 DSL 由 builder 编译为 `WorkflowDefinition`，内部生成稳定 node id、节点类型、节点配置、join 条件和边。

### 显式 id 写法

```java
flow.from("leaveApproval")
        .label("发起请假审批")
        .join(all(
                node("hrApproval", "HR 审批", approval(...)),
                node("managerApproval", "部门主管审批", approval(...))
        ))
        .on("审批通过").to("onLeave")
        .on("审批拒绝").to("requestLeave")
        .on("打回").to("requestLeave");
```

页面配置应优先使用显式 id。简洁写法只作为 Java DSL 语法糖。

### 条件路由

```java
flow.from("金额判断")
        .when("${ctx.amount > 10000}").to("大额审批")
        .otherwise().to("普通审批");
```

条件表达式属于流程图连线或 gateway，不属于具体节点类型。

## taskGroup 语义

`taskGroup` 是一个复合节点，负责：

1. 进入节点时创建子任务。
2. 记录每个子任务的 `NodeResult`。
3. 根据 join 规则判断自身是否完成。
4. 产出自身 outcome。
5. 由外部 edge 决定主流程下一节点。

例如：

```java
join(all(
        node("HR 审批", approval(...)),
        node("部门主管审批", approval(...))
))
```

可默认解释为：

- 所有子节点成功类 outcome 满足时，taskGroup 产出 `审批通过`。
- 任意子节点拒绝类 outcome 满足时，taskGroup 产出 `审批拒绝`。
- 任意子节点打回类 outcome 满足时，taskGroup 产出 `打回`。

默认映射可以由 `approvalTask` 的 outcome 契约提供，也允许高级场景显式覆盖。

## 页面配置模型

页面配置不保存 Java 对象，只保存 `WorkflowDefinition` JSON。

示例：

```json
{
  "id": "leave",
  "name": "请假流程",
  "nodes": [
    {
      "id": "requestLeave",
      "label": "发起请假",
      "type": "serviceTask",
      "templateId": null,
      "config": {
        "activity": "createLeaveRequest"
      }
    },
    {
      "id": "leaveApproval",
      "label": "发起请假审批",
      "type": "taskGroup",
      "templateId": null,
      "config": {
        "join": {
          "type": "all"
        },
        "tasks": [
          {
            "id": "hrApproval",
            "label": "HR 审批",
            "type": "approvalTask",
            "config": {
              "assignee": "${ctx.hrId}"
            }
          },
          {
            "id": "managerApproval",
            "label": "部门主管审批",
            "type": "approvalTask",
            "config": {
              "assignee": "${ctx.departmentManagerId}"
            }
          }
        ]
      }
    }
  ],
  "edges": [
    {
      "from": "requestLeave",
      "to": "leaveApproval",
      "event": "提交",
      "outcome": null,
      "condition": null
    },
    {
      "from": "leaveApproval",
      "to": "onLeave",
      "event": null,
      "outcome": "审批通过",
      "condition": null
    },
    {
      "from": "leaveApproval",
      "to": "requestLeave",
      "event": null,
      "outcome": "审批拒绝",
      "condition": null
    }
  ]
}
```

## 运行时语义

运行时只按统一流程执行：

1. 进入当前节点。
2. 根据 `NodeDefinition.type` 查找 `NodeHandler`。
3. 将 `JsonNode config` 转成 handler 的配置类型。
4. 调用 `run(ctx, config)`。
5. 如果返回 `WAITING`，实例挂起，等待 signal、timer 或子任务结果。
6. 如果返回 `COMPLETED`，根据 `outcome` 匹配 edge，进入下一节点。
7. 如果返回 `FAILED`，按失败策略处理，并触发补偿。
8. 如果节点是 `taskGroup`，运行时维护 active child tasks 和子任务结果。

节点不直接决定下一节点；下一节点由 edge 决定。

## 图能力

为页面配置和运行时校验提供以下能力：

- `validateDefinition(definition)`
- `canReach(fromNodeId, toNodeId)`
- `canAccept(currentNodeId, event)`
- `nextCandidates(currentNodeId, context)`
- `listIncoming(nodeId)`
- `listOutgoing(nodeId)`
- `detectCycle()`
- `validateNodeConfig(nodeDefinition)`

## Temporal 适配

核心模型不暴露 Temporal API。Temporal runtime adapter 负责：

- workflow instance 持久化。
- signal 接收和分发。
- timer 调度。
- activity 调用。
- saga compensation。
- taskGroup 子任务等待。

`NodeHandler.compensable()` 和 `compensate(...)` 是核心抽象，Temporal 适配层可以把它们映射到 `Saga`。

## 与当前 V1 的关系

当前 V1 API：

```java
flow.from(A).to(B).on(EVENT).perform(activity);
flow.approval(APPROVAL_STATE).expr(...).onApproved(...).onRejected(...);
```

建议保留为 legacy，后续迁移为 V2 builder 的语法糖。

迁移方向：

- `from/to/on/perform` 可迁移为 `serviceTask + edge`。
- `approval(state)` 不再作为 V2 主 API，改为 `approvalTask` 或 `taskGroup` 内的子任务。
- `ProcessDefinition` 可逐步迁移到 `WorkflowDefinitionProvider`。

## 实施计划

### 阶段 1：设计定稿

- 完成本文档评审。
- 明确 `NodeDefinition`、`EdgeDefinition`、`NodeResult` 字段。
- 明确 `taskGroup` 默认 join 语义。
- 明确内置节点类型的 outcome 契约。

### 阶段 2：核心定义模型

- 新增 workflow v2 definition 包。
- 实现 `WorkflowDefinition`、`NodeDefinition`、`EdgeDefinition`。
- 实现 JSON 序列化和反序列化测试。
- 实现基础 definition 校验。

### 阶段 3：Node SPI

- 实现 `NodeHandler`、`NodeHandlerRegistry`。
- 实现 `NodeResult`、`WorkflowEvent`、`NodeExecutionContext`。
- 提供内置节点 handler 的最小版本。

### 阶段 4：Java DSL

- 实现 builder，输出 `WorkflowDefinition`。
- 支持 `from/on/to`、`join(all(...))`、`join(any(...))`。
- 支持简洁 label 写法和显式 id 写法。

### 阶段 5：图能力

- 实现流程校验。
- 实现可达性、事件接收判断、候选下一节点判断。
- 为页面配置提供错误信息。

### 阶段 6：Temporal Runtime

- 实现 V2 generic workflow。
- 实现进入节点、等待事件、路由下一节点。
- 实现 `taskGroup` 子任务聚合。
- 实现补偿映射。

### 阶段 7：迁移与文档

- 标记 V1 API 为 legacy。
- 增加请假审批、采购审批、定时等待示例。
- 更新 `doc/AI_MODULE_GUIDE.md` 中过期的 workflow 说明。

## 待确认问题

1. `taskGroup` 默认 outcome 名称是否使用中文业务词，例如 `审批通过`，还是使用稳定英文常量，例如 `APPROVED`。
2. 页面配置中的表达式语言使用什么方案：简单模板表达式、MVEL、JEXL、SpEL，还是自研最小表达式。
3. `serviceTask` 如何注册业务 activity：字符串名称注册，还是 Java bean/class 引用。
4. 子任务是否需要独立生命周期和审计表，还是作为主流程 runtime 的嵌套记录。
5. V2 是否直接放在 `infrastructure-component-workflow-temporal`，还是先拆出不依赖 Temporal 的 workflow core 模块。
