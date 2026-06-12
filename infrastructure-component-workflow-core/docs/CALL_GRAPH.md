# workflow-core 调用关系图

本文只覆盖 `infrastructure-component-workflow-core/src/main/java`，用于快速上手修改 core。

## 总览

```mermaid
flowchart LR
    DSL["dsl<br/>Flow / FlowDef / FlowFrom / FlowJoin / NodeBuilder / NodeConfig"]
    DEF["definition<br/>WorkflowDefinition / NodeDefinition / EdgeDefinition / NodeResult"]
    GRAPH["graph<br/>WorkflowGraph / GraphValidator"]
    ENGINE["engine<br/>NodeConfigCodec / EdgeRouter"]
    SPI["spi<br/>NodeHandler / NodeHandlerRegistry / NodeSpec / ConditionEvaluator"]
    HANDLERS["handlers<br/>BuiltInHandlers / BuiltInNodeSpecs / *Handler / *Config"]
    INTC["interceptor<br/>WorkflowInterceptorRegistry / hooks"]

    DSL --> DEF
    GRAPH --> DEF
    GRAPH --> SPI
    ENGINE --> DEF
    ENGINE --> GRAPH
    ENGINE --> SPI
    HANDLERS --> DEF
    HANDLERS --> SPI
    INTC --> DEF
    INTC --> SPI
```

核心数据流：

1. DSL 构造 `WorkflowDefinition`。
2. `GraphValidator` 校验定义结构、handler 注册和 handler config 反序列化。
3. runtime 通过 `NodeConfigCodec.decode` 得到 typed config。
4. runtime 通过 `NodeConfigCodec.decodeState` 从 `businessData` 得到 handler 请求的 typed state。
5. runtime 调用 `NodeHandler.run(ctx, config, state)`；等待事件时先用三参 `canAccept` 过滤，再用 `decodeEventPayload` 得到 typed event payload。
6. runtime 调用 `NodeHandler.canAccept(ctx, event, config, eventPayload)` 和 `NodeHandler.onEvent(ctx, event, config, eventPayload)`。
7. runtime 用 `EdgeRouter.pickNextEdge` 决定下一个节点。

## DSL 构建链

```mermaid
flowchart TD
    A["Flow.define(id, name)"] --> B["new FlowDef(id, name)"]
    B --> C["FlowDef.start(name)"]
    B --> D["FlowDef.node(name, NodeConfig)"]
    B --> E["FlowDef.node(id, Consumer<NodeBuilder>)"]
    E --> E1["new NodeBuilder()"]
    E1 --> E2["configurer.accept(b)"]
    E2 --> E3["NodeBuilder.type(...) / config(NodeSpec&lt;C&gt;, C) / label(...) / set(...) / setAll(...)"]
    E3 --> E5["NodeBuilder.config(...) -> NodeConfig.of(spec, config)"]
    E3 --> E4["NodeBuilder.buildConfig()"]
    E5 --> E4
    E4 --> D

    B --> F["FlowDef.from(name)"]
    F --> G["new FlowFrom(flowDef, name)"]
    G --> H["FlowFrom.on(event)"]
    H --> I["FlowTo.to(dest)"]
    H --> J["FlowTo.when(elExpression)"]
    J --> K["WhenFlowTo.to(dest)"]
    I --> L["FlowFrom.registerEdge(event, dest)"]
    K --> M["FlowFrom.registerEdge(event, dest, when)"]
    L --> N["FlowDef.registerEdge(EdgeDefinition)"]
    M --> N

    G --> O["FlowFrom.join(JoinSpec)"]
    O --> P["FlowDef.registeredNonTaskGroupType(fromNodeName)"]
    O --> Q["FlowDef.registerJoinSpec(fromNodeName, spec)"]
    O --> R["FlowDef.ensureNode(fromNodeName, taskGroup NodeConfig)"]
    O --> S["new FlowJoin(flowDef, fromNodeName)"]
    S --> T["FlowJoin.on(event)"]
    T --> U["FlowJoinTo.to(dest)"]
    U --> V["FlowJoin.registerEdge(event, dest)"]
    V --> N

    B --> W["FlowDef.build()"]
    W --> X["collect nodeConfigs keys"]
    W --> Y["add edge.from / edge.to"]
    W --> Z["for each node name"]
    Z --> Z1["joinSpecs contains name -> buildTaskGroupConfig(spec)"]
    Z1 --> Z2["recursive buildTaskGroupConfig(child.nestedJoin())"]
    Z --> Z3["nodeConfigs contains name -> MAPPER.valueToTree(cfg.props())"]
    Z --> Z4["implicit edge-only node -> END_TASK"]
    Z1 --> AA["new NodeDefinition(...)"]
    Z3 --> AA
    Z4 --> AA
    AA --> AB["new WorkflowDefinition(workflowId, workflowName, nodes, edges, startNodeId)"]
```

节点 config 类型边界：

```mermaid
flowchart LR
    A["NodeSpec&lt;C&gt;<br/>type string + configType"] --> B["NodeBuilder.config(spec, C config)"]
    B --> C["NodeConfig.of(spec, config)"]
    C --> D["Jackson convertValue(config)<br/>Map&lt;String,Object&gt; props"]
    D --> E["FlowDef.build()<br/>MAPPER.valueToTree(props)"]
    E --> F["NodeDefinition.config()<br/>JsonNode"]
    F --> G["NodeConfigCodec.decode(mapper, handler, rawConfig)"]
    G --> H["handler.configType()<br/>typed CONFIG"]
```

`NodeSpec<C>` 只约束 Java DSL 调用侧，`WorkflowDefinition` 仍保存 JSON；这保证可视化、持久化和跨 runtime 传输不依赖 Java 泛型。

DSL 静态工厂：

| 方法 | 直接创建/调用 |
|---|---|
| `Flow.define(String, String)` | `new FlowDef(...)` |
| `Dsl.node(String, NodeConfig)` | `new ChildNodeSpec(name, config)` |
| `Dsl.node(String, NodeConfig, JoinSpec)` | `new ChildNodeSpec(name, config, nestedJoin)` |
| `Dsl.node(String, Consumer<NodeBuilder>)` | `new NodeBuilder()` -> `configurer.accept(b)` -> `b.buildConfig()` -> `new ChildNodeSpec(...)`；子节点也可以在 builder 内调用 `NodeBuilder.config(NodeSpec<C>, C)` |
| `Dsl.all(ChildNodeSpec...)` | `new JoinSpec("all", Arrays.asList(nodes))` |
| `Dsl.any(ChildNodeSpec...)` | `new JoinSpec("any", Arrays.asList(nodes))` |
| `BuiltInNodes.service(...)` | returns `Consumer<NodeBuilder>` that calls `NodeBuilder.config(BuiltInNodeSpecs.SERVICE_TASK, new ServiceTaskConfig(...))` |
| `BuiltInNodes.approval(...)` | returns `Consumer<NodeBuilder>` that calls `NodeBuilder.config(BuiltInNodeSpecs.APPROVAL_TASK, new ApprovalTaskConfig(...))` |
| `BuiltInNodes.userTask(...)` | returns `Consumer<NodeBuilder>` that calls `NodeBuilder.config(BuiltInNodeSpecs.USER_TASK, new UserTaskConfig(...))` |
| `BuiltInNodes.event(...)` | returns `Consumer<NodeBuilder>` that calls `NodeBuilder.config(BuiltInNodeSpecs.EVENT_TASK, new EventTaskConfig(...))` |
| `BuiltInNodes.timer(...)` | returns `Consumer<NodeBuilder>` that calls `NodeBuilder.config(BuiltInNodeSpecs.TIMER_TASK, new TimerTaskConfig(...))` |
| `BuiltInNodes.subProcess(...)` | returns `Consumer<NodeBuilder>` that calls `NodeBuilder.config(BuiltInNodeSpecs.SUB_PROCESS, new SubProcessConfig(...))` |

`gateway` / `endTask` / `taskGroup` 没有额外配置 sugar。直接写 `b -> b.config(BuiltInNodeSpecs.GATEWAY, new GatewayConfig(...))`、`flow.endNode(name)`，或通过 `flow.from(name).join(...)` 生成 `taskGroup`。

## 定义模型

```mermaid
classDiagram
    class WorkflowDefinition {
        +String id()
        +String name()
        +List~NodeDefinition~ nodes()
        +List~EdgeDefinition~ edges()
        +String startNodeId()
        +of(id, name, nodes, edges)
    }
    class NodeDefinition {
        +String id()
        +String label()
        +String type()
        +String templateId()
        +JsonNode config()
    }
    class EdgeDefinition {
        +String from()
        +String to()
        +String event()
        +String when()
    }
    class NodeResult {
        +NodeStatus status()
        +String outcome()
        +Map payload()
        +String message()
        +waiting()
        +completed(outcome)
        +completed(outcome, payload)
        +failed(message)
        +cancelled()
    }
    class ExecutionRecord {
        +String nodeId()
        +int visitNo()
        +NodeResult result()
    }
    WorkflowDefinition --> NodeDefinition
    WorkflowDefinition --> EdgeDefinition
    ExecutionRecord --> NodeResult
```

构造器校验：

| 记录 | 构造器行为 |
|---|---|
| `WorkflowDefinition` | 校验 `id`、`name`、`nodes`、`edges`，复制 list，校验非空 `startNodeId` 必须存在于 `nodes` |
| `NodeDefinition` | 校验 `id`、`type` |
| `EdgeDefinition` | 校验 `from`、`to` |
| `NodeResult` | 校验 `status`，复制 `payload`，`payload == null` 时转为 `Map.of()` |
| `ExecutionRecord` | 仅保存一次节点访问记录 |

## 图索引和校验

```mermaid
flowchart TD
    A["GraphValidator.validate(definition, registry)"] --> B["check workflow id/name"]
    B --> C["check nodes not empty"]
    C --> D["check duplicate node id and blank type"]
    D --> E["check edge.from / edge.to references"]
    E --> F["registry != null"]
    F --> G["for each node: registry.find(node.type())"]
    G --> H["validateConfigBinding(node, handler)"]
    H --> I["NodeConfigCodec.decode(MAPPER, handler, node.config)"]
    I --> I1["normalize taskGroup DSL shape"]
    I --> I2["mapper.treeToValue(normalized, handler.configType())"]
    E --> N["new WorkflowGraph(definition)"]
    N --> O["resolve explicit/topological starts"]
    N --> P["graph.endNodes()"]
    N --> Q["graph.reachableFrom(start roots)"]
    N --> R["graph.detectCycles()"]
    O --> S["new ValidationReport(errors, warnings)"]
    P --> S
    Q --> S
    R --> S
    S --> T["ValidationReport.ok()"]
    S --> U["ValidationReport.throwIfInvalid()"]
```

`GraphValidator.validate(definition, registry)` 的 registry 分支只校验节点 type 是否能找到 handler，以及节点 config 是否能绑定到 `handler.configType()`。边上的 `event` 是 `.on(...)` 写入的路由 key，不再要求 handler 预声明 outcome 集合。

`WorkflowGraph` 方法关系：

```mermaid
flowchart TD
    A["new WorkflowGraph(definition)"] --> B["index nodesById"]
    A --> C["index outgoingByNodeId"]
    A --> D["index incomingByNodeId"]

    E["node(nodeId)"] --> E1["nodesById.get(nodeId)"]
    F["findNode(nodeId)"] --> F1["Optional.ofNullable(nodesById.get(nodeId))"]
    G["nodeIds()"] --> G1["nodesById.keySet()"]

    H["outgoing(nodeId)"] --> H1["requireKnownNode(nodeId)"]
    H1 --> H2["outgoingByNodeId.getOrDefault(...)"]
    I["incoming(nodeId)"] --> I1["requireKnownNode(nodeId)"]
    I1 --> I2["incomingByNodeId.getOrDefault(...)"]

    J["nextCandidates(currentNodeId, match)"] --> H
    J --> J1["EdgeMatch.Any -> return all outgoing"]
    J --> J2["EdgeMatch.ByEvent -> edge.event equals eventName"]
    K["canAccept(currentNodeId, event)"] --> J

    L["canReach(fromNodeId, toNodeId)"] --> H1
    L --> I1
    L --> M["reachableFrom(fromNodeId)"]
    M --> H2

    N["detectCycles()"] --> O["strongConnect(id, state)"]
    O --> P["iteratorOf(nodeId)"]
    O --> Q["hasSelfLoop(only)"]
    N --> R["stableOrder(scc)"]

    S["startNodes()"] --> T["hasNoExternalIncoming(id)"]
    U["effectiveStartNode()"] --> S
    V["endNodes()"] --> V1["outgoingByNodeId empty OR node.type == END_TASK"]
```

## engine 辅助逻辑

```mermaid
flowchart TD
    A["NodeConfigCodec.normalizeDefinition(definition)"] --> B["for each NodeDefinition"]
    B --> C["normalizeConfig(node.type, node.config)"]
    C --> D["TASK_GROUP join object -> replace join with join.type text"]
    C --> E["other shape -> keep rawConfig"]
    D --> F["new NodeDefinition(...)"]
    E --> G["return original node"]
    F --> H["new WorkflowDefinition(...)"]

    I["NodeConfigCodec.decode(mapper, handler, rawConfig)"] --> J["handler.configType()"]
    J --> K["Void/null -> return null"]
    J --> L["normalizeConfig(handler.type(), rawConfig)"]
    L --> M["null config -> mapper.createObjectNode()"]
    M --> N["mapper.treeToValue(normalized, configType)"]

    O["NodeConfigCodec.decodeState(mapper, handler, businessData)"] --> P["handler.stateType()"]
    Q["NodeConfigCodec.decodeEventPayload(mapper, handler, payload)"] --> R["handler.eventType()"]
    P --> S["decodeJsonValue(...)"]
    R --> S
    S --> T["Void/null -> return null"]
    S --> U["JsonNode target -> return JsonNode source"]
    S --> V["mapper.treeToValue(source, targetType)"]
```

```mermaid
flowchart TD
    A["EdgeRouter.resolveStartNode(requested, definition, graph)"] --> B["requested nonblank -> requested"]
    A --> C["definition.startNodeId nonblank -> startNodeId"]
    A --> D["graph.startNodes()"]
    D --> E["size == 1 -> only start"]
    D --> F["otherwise throw IllegalStateException"]

    G["EdgeRouter.pickNextEdge(graph, currentNodeId, outcome, ctx)"] --> H["outcome != null"]
    H --> I["for edge in graph.outgoing(currentNodeId)"]
    I --> J["outcome equals edge.event"]
    J --> K["matchesCondition(edge.when, ctx)"]
    K --> L["ConditionEvaluator.evaluateOrTrue(...)"]
    G --> M["fallback: first outgoing edge with event/when null"]
    M --> N["return edge or null"]
```

## SPI 注册、确定性扫描、条件表达式

```mermaid
flowchart TD
    A["NodeHandlerRegistry.register(handler)"] --> B["validateType(handler)"]
    B --> C["DeterminismGuard.assertPure(handler.getClass())"]
    C --> D["DeterminismGuard.staticScan(handlerClass)"]
    D --> E["readClassFile(handlerClass)"]
    E --> F["ConstantPool.parse(bytes)"]
    F --> G["referencedClassNames()"]
    F --> H["referencedMethods()"]
    G --> I["compare FORBIDDEN_TYPES"]
    H --> J["compare FORBIDDEN_METHODS"]
    C --> K["putHandler(type, handler)"]

    L["NodeHandlerRegistry.registerBuiltIn(handler)"] --> B
    L --> K
    M["lookup(type)"] --> N["handlers.get(type) or throw"]
    O["find(type)"] --> P["Optional.ofNullable(handlers.get(type))"]
```

```mermaid
flowchart TD
    A["ConditionEvaluator.evaluate(condition, ctx)"] --> C["evaluateNonBlank(trimmed, ctx)"]
    B["ConditionEvaluator.evaluateOrTrue(condition, ctx)"] --> C
    C --> D["literal true/false"]
    C --> E["must start with ${ and end with }"]
    E --> F["BARE_IDENT regex"]
    F --> G["ctx.variables().get(name)"]
    G --> H["toBoolean(value)"]
    E --> I["new StandardELContext(FACTORY)"]
    I --> J["add JsonNodeELResolver"]
    I --> K["add ResultsELResolver"]
    I --> L["VariableMapper: variables/businessData/results/ctx"]
    L --> M["FACTORY.createValueExpression(el, trimmed, Object.class)"]
    M --> N["ve.getValue(el)"]
    N --> H
```

EL resolver 调用关系：

| 类 | `getValue` 输入 | 输出 |
|---|---|---|
| `JsonNodeELResolver` | `base instanceof JsonNode` + `property` | boolean/number/text scalar, or child `JsonNode` |
| `ResultsELResolver` | `base instanceof ConditionEvaluator.ResultsView` | `ResultsView.get(property.toString())` |
| `ResultsELResolver` | `base instanceof NodeResult` | `outcome` / `status` / `payload` / `message` / `payload.get(property)` |

## 内置 handler 统一执行模型

`NodeHandler<CONFIG, STATE, EVENT>` 的三个泛型只影响 Java 侧 handler 入口类型，不改变流程定义 JSON：

| 泛型 | JSON 来源 | 解码方法 | 主要入口 |
|---|---|---|---|
| `CONFIG` | `NodeDefinition.config()` | `NodeConfigCodec.decode(...)` | `run` / `canAccept` / `onEvent` / `compensate` |
| `STATE` | `NodeExecutionContext.businessData()` | `NodeConfigCodec.decodeState(...)` | `run(ctx, config, state)` / `compensate(ctx, config, completedResult, state)` |
| `EVENT` | `WorkflowEvent.payload()` | `NodeConfigCodec.decodeEventPayload(...)` | `canAccept(ctx, event, config, eventPayload)` / `onEvent(ctx, event, config, eventPayload)` |

```mermaid
classDiagram
    class NodeHandler~CONFIG STATE EVENT~ {
        +String type()
        +Class~CONFIG~ configType()
        +Class~STATE~ stateType()
        +Class~EVENT~ eventType()
        +NodeResult run(ctx, CONFIG)
        +NodeResult run(ctx, CONFIG, STATE)
        +boolean canAccept(ctx, event, CONFIG)
        +boolean canAccept(ctx, event, CONFIG, EVENT)
        +NodeResult onEvent(ctx, event, CONFIG)
        +NodeResult onEvent(ctx, event, CONFIG, EVENT)
        +boolean compensable()
        +NodeResult compensate(ctx, CONFIG, completedResult)
        +NodeResult compensate(ctx, CONFIG, completedResult, STATE)
    }
    class NodeExecutionContext {
        +JsonNode businessData()
        +Map variables()
        +NodeResult resultOf(nodeId)
        +WorkflowEvent currentEvent()
    }
    class WorkflowEvent {
        +String name()
        +JsonNode payload()
        +String eventId()
    }
    NodeHandler --> NodeExecutionContext
    NodeHandler --> WorkflowEvent
```

兼容关系：

1. 已有内置 handler 仍实现二参 `run(ctx, config)`、三参 `canAccept(ctx, event, config)`、三参 `onEvent(ctx, event, config)`。
2. runtime 调用 typed 重载；默认实现会回落到旧入口。
3. 自定义 handler 需要 typed state/event 时，覆盖 `stateType()` / `eventType()` 和对应 typed 重载即可。
4. 事件匹配顺序是三参 `canAccept` 先执行；只有返回 `true` 时才解码 `EVENT` 并调用四参 `canAccept`。

runtime 对 core handler 的典型调用顺序：

```mermaid
sequenceDiagram
    participant R as runtime adapter
    participant REG as NodeHandlerRegistry
    participant C as NodeConfigCodec
    participant H as NodeHandler
    participant ER as EdgeRouter

    R->>REG: lookup(node.type)
    REG-->>R: NodeHandler
    R->>C: decode(mapper, handler, node.config)
    C-->>R: typed config
    R->>C: decodeState(mapper, handler, ctx.businessData)
    C-->>R: typed state
    R->>H: run(ctx, config, state)
    alt result.status == WAITING
        R->>R: perform RuntimeIntents side effect
        R->>H: canAccept(ctx, event, config)
        R->>C: decodeEventPayload(mapper, handler, event.payload)
        C-->>R: typed event payload
        R->>H: canAccept(ctx, event, config, eventPayload)
        R->>H: onEvent(ctx, event, config, eventPayload)
    end
    R->>ER: pickNextEdge(graph, nodeId, result.outcome, ctx)
    ER-->>R: EdgeDefinition or null
```

各内置 handler 方法关系：

```mermaid
flowchart TD
    A["BuiltInHandlers.registerAll(registry)"] --> A1["registry.registerBuiltIn(new ServiceTaskHandler())"]
    A --> A2["registry.registerBuiltIn(new ApprovalTaskHandler())"]
    A --> A3["registry.registerBuiltIn(new UserTaskHandler())"]
    A --> A4["registry.registerBuiltIn(new EventTaskHandler())"]
    A --> A5["registry.registerBuiltIn(new TimerTaskHandler())"]
    A --> A6["registry.registerBuiltIn(new TaskGroupHandler())"]
    A --> A7["registry.registerBuiltIn(new GatewayHandler())"]
    A --> A8["registry.registerBuiltIn(new SubProcessHandler())"]
    A --> A9["registry.registerBuiltIn(new EndTaskHandler())"]
```

### `ServiceTaskHandler`

```mermaid
flowchart TD
    A["run(ctx, ServiceTaskConfig)"] --> B["payload[RuntimeIntents.ACTIVITY] = config.activity"]
    A --> C["payload[RuntimeIntents.ACTIVITY_INPUT] = config.activityInput"]
    B --> D["new NodeResult(WAITING, null, payload, null)"]
    C --> D
    E["canAccept(ctx, event, cfg)"] --> F["event.name == _activityResult"]
    G["onEvent(ctx, event, config)"] --> H["payload.status == success"]
    H --> I["NodeResult.completed(SUCCESS, output?)"]
    H --> J["NodeResult.failed(message)"]
    K["compensate(ctx, config, completedResult)"] --> L["blank compensateActivity -> NodeResult.completed(SUCCESS)"]
    K --> M["payload[RuntimeIntents.ACTIVITY] = compensateActivity"]
    M --> N["new NodeResult(WAITING, null, payload, null)"]
```

### `ApprovalTaskHandler`

```mermaid
flowchart TD
    A["run(ctx, ApprovalTaskConfig)"] --> B["payload[ASSIGNEE] = config.assignee"]
    A --> C["payload[AWAIT_EVENT] = awaitEventName(config)"]
    A --> D["payload[TIMEOUT_DURATION] = config.timeoutDuration"]
    B --> E["new NodeResult(WAITING, null, payload, null)"]
    C --> E
    D --> E

    F["canAccept(ctx, event, config)"] --> G["event.name == _timeout -> true"]
    F --> H["event.name == awaitEventName(config)"]
    H --> I["matchesTaskId(ctx, event)"]
    I --> I1["no taskId -> true"]
    I --> I2["taskId equals currentNodeId -> true"]
    I --> I3["currentNodeId endsWith /taskId -> true"]

    J["onEvent(ctx, event, config)"] --> K["_timeout -> completed(TIMEOUT)"]
    J --> L["decision == approved -> completed(APPROVED)"]
    J --> M["decision == rejected -> completed(REJECTED)"]
    J --> N["decision == sendback -> completed(SENDBACK)"]
    J --> O["missing/unknown decision -> failed(...)"]
```

### `UserTaskHandler`

```mermaid
flowchart TD
    A["run(ctx, UserTaskConfig)"] --> B["payload[ASSIGNEE] / payload[AWAIT_EVENT] / payload[TIMEOUT_DURATION]"]
    B --> C["new NodeResult(WAITING, null, payload, null)"]
    D["canAccept(ctx, event, config)"] --> E["_timeout -> true"]
    D --> F["event.name == awaitEventName(config)"]
    F --> G["matchesTaskId(ctx, event)"]
    H["onEvent(ctx, event, config)"] --> I["_timeout -> completed(TIMEOUT)"]
    H --> J["decision == completed -> completed(COMPLETED)"]
    H --> K["decision == cancelled -> completed(CANCELLED)"]
    H --> L["missing/unknown decision -> failed(...)"]
```

### `EventTaskHandler`

```mermaid
flowchart TD
    A["run(ctx, EventTaskConfig)"] --> B["payload[AWAIT_EVENT] = config.awaitEvent"]
    A --> C["payload[TIMEOUT_DURATION] = config.timeoutDuration"]
    B --> D["new NodeResult(WAITING, null, payload, null)"]
    C --> D
    E["canAccept(ctx, event, config)"] --> F["event.name == _timeout OR event.name == config.awaitEvent"]
    G["onEvent(ctx, event, config)"] --> H["_timeout -> completed(TIMEOUT)"]
    G --> I["awaitEvent match -> completed(RECEIVED)"]
    G --> J["otherwise waiting()"]
```

### `TimerTaskHandler`

```mermaid
flowchart TD
    A["run(ctx, TimerTaskConfig)"] --> B["payload[DURATION] = config.duration"]
    B --> C["new NodeResult(WAITING, null, payload, null)"]
    D["canAccept(ctx, event, config)"] --> E["event.name == _timerFired OR _cancel"]
    F["onEvent(ctx, event, config)"] --> G["_timerFired -> completed(TRIGGERED)"]
    F --> H["_cancel -> completed(CANCELLED)"]
    F --> I["otherwise waiting()"]
```

### `TaskGroupHandler`

```mermaid
flowchart TD
    A["run(ctx, TaskGroupConfig)"] --> B["effectiveJoinRule(config)"]
    B --> C["payload[JOIN_RULE] = rule.wireName()"]
    A --> D["serialize config.tasks into payload[CHILDREN]"]
    C --> E["new NodeResult(WAITING, null, payload, null)"]
    D --> E

    F["canAccept(ctx, event, cfg)"] --> G["event.name == _childCompleted"]
    H["onEvent(ctx, event, cfg)"] --> I["empty tasks -> completed(APPROVED)"]
    H --> J["effectiveJoinRule(cfg)"]
    J --> K["for each child -> ctx.resultOf(TaskGroupContract.childKey(parent, child.id))"]
    K --> L["aggregate(rule, childResults)"]
    L --> M["ALL: rejected/sendback short-circuit"]
    L --> N["ALL: all approved-like -> APPROVED, otherwise WAITING"]
    L --> O["ANY: first approved-like -> APPROVED"]
    L --> P["ANY: unfinished child -> WAITING"]
    L --> Q["ANY: all done and none approved -> REJECTED"]
```

### `GatewayHandler`

```mermaid
flowchart TD
    A["run(ctx, GatewayConfig)"] --> B["for branch in config.branches"]
    B --> C["ConditionEvaluator.evaluate(branch.when, ctx)"]
    C --> D["true -> NodeResult.completed(branch.outcome)"]
    C --> E["false -> next branch"]
    E --> F["defaultOutcome nonblank -> completed(defaultOutcome)"]
    E --> G["no branch/default -> failed(no branch matched)"]
```

### `SubProcessHandler`

```mermaid
flowchart TD
    A["run(ctx, SubProcessConfig)"] --> B["payload[SUB_WORKFLOW_ID] = config.subWorkflowId"]
    A --> C["payload[SUB_DEFINITION_JSON] = config.definitionJson"]
    A --> D["payload[SUB_INPUT] = config.input"]
    B --> E["new NodeResult(WAITING, null, payload, null)"]
    C --> E
    D --> E
    F["canAccept(ctx, event, cfg)"] --> G["event.name == _subProcessCompleted"]
    H["onEvent(ctx, event, cfg)"] --> I["payload.subOutcome missing -> failed(...)"]
    H --> J["cfg.outcomeMapping overrides subOutcome"]
    J --> K["NodeResult.completed(mapped)"]
```

### `EndTaskHandler`

```mermaid
flowchart TD
    A["run(ctx, EndTaskConfig)"] --> B["NodeResult.completed(COMPLETED)"]
```

## interceptor

```mermaid
flowchart TD
    A["WorkflowInterceptorRegistry.register(interceptor)"] --> B["interceptor instanceof DeterministicInterceptor"]
    A --> C["interceptor instanceof AsyncInterceptor"]
    B --> D["insertSorted(deterministic, di)"]
    C --> E["insertSorted(async, ai)"]
    A --> F["neither -> IllegalArgumentException"]
    G["deterministic()"] --> H["List.copyOf(deterministic)"]
    I["async()"] --> J["List.copyOf(async)"]
    K["clear()"] --> L["deterministic.clear(); async.clear()"]
```

Hook 方法名在两个接口中一致：

| 接口 | hook 方法 |
|---|---|
| `DeterministicInterceptor` | `onWorkflowStart` / `onWorkflowEnd` / `onNodeEnter` / `onNodeExit` / `onNodeError` / `onEvent` / `onCompensate` |
| `AsyncInterceptor` | `onWorkflowStart` / `onWorkflowEnd` / `onNodeEnter` / `onNodeExit` / `onNodeError` / `onEvent` / `onCompensate` |

## 修改入口索引

| 要改的能力 | 主要文件 | 先看方法 |
|---|---|---|
| 新增内置节点类型 | `handlers/*Handler.java`, `handlers/*Config.java`, `spi/NodeTypes.java`, `spi/BuiltInNodeType.java`, `spi/NodeSpec.java`, `handlers/BuiltInNodeSpecs.java`, `handlers/BuiltInHandlers.java`, `dsl/BuiltInNodes.java` | `NodeHandler.run`, `NodeHandler.canAccept`, `NodeHandler.onEvent`, `BuiltInHandlers.registerAll` |
| 修改 handler 泛型输入 | `spi/NodeHandler.java`, `engine/NodeConfigCodec.java` | `configType`, `stateType`, `eventType`, `decode`, `decodeState`, `decodeEventPayload` |
| 修改 DSL 输出 JSON | `dsl/FlowDef.java`, `dsl/NodeBuilder.java`, `dsl/NodeConfig.java`, `dsl/Dsl.java` | `FlowDef.build`, `FlowDef.buildTaskGroupConfig`, `NodeConfig.of`, `NodeBuilder.buildConfig` |
| 修改流程图校验 | `graph/GraphValidator.java`, `graph/WorkflowGraph.java` | `GraphValidator.validate`, `GraphValidator.validateNodeConfig`, `WorkflowGraph.detectCycles` |
| 修改路由规则 | `engine/EdgeRouter.java`, `graph/WorkflowGraph.java` | `EdgeRouter.pickNextEdge`, `WorkflowGraph.nextCandidates` |
| 修改表达式能力 | `spi/ConditionEvaluator.java`, `spi/JsonNodeELResolver.java`, `spi/ResultsELResolver.java` | `ConditionEvaluator.evaluateNonBlank`, resolver `getValue` |
| 修改自定义 handler 注册规则 | `spi/NodeHandlerRegistry.java`, `spi/DeterminismGuard.java` | `NodeHandlerRegistry.register`, `DeterminismGuard.staticScan` |
| 修改会签聚合 | `handlers/TaskGroupHandler.java`, `handlers/TaskGroupContract.java` | `TaskGroupHandler.onEvent`, `TaskGroupHandler.aggregate`, `TaskGroupContract.childKey` |
| 修改生命周期 hook | `interceptor/*` | `WorkflowInterceptorRegistry.register`, `insertSorted` |
