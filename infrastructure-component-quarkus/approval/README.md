# infrastructure-component-quarkus / approval

为 Quarkus CDI 应用提供线性多节点审批流能力：通过 REST API 发起审批实例、执行审批/退回/转交动作，
并记录完整的操作历史。审批流按节点列表顺序推进，持久化由底层 `persistence` 模块（jOOQ + `DSLContext`）驱动。

---

## 依赖

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:approval"))
}
```

本模块间接引入 `libs.bundles.web`（`quarkus-rest`、`quarkus-jackson`、`quarkus-smallrye-openapi`、
`quarkus-reactive-routes`、`quarkus-vertx`、`quarkus-smallrye-fault-tolerance`、`quarkus-httpclient`）
以及 `infrastructure-component-quarkus:persistence`（提供 `DSLContext` bean）。

---

## 构建开关

`ApprovalService` bean 受编译期属性控制，**默认不激活**，需在 `application.properties` 中显式开启：

```properties
quarkus.plugins.approval.enable=true
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `quarkus.plugins.approval.enable` | `String` | （未设置） | 设为 `"true"` 时才将 `ApprovalService` 注册为 CDI bean |

> 这是 `@IfBuildProperty` 构建时开关，**更改后需重新编译**，运行期修改无效。

---

## 数据模型

### 枚举

**`ApprovalStatus`** — 审批实例状态

| 枚举值 | 含义 |
|--------|------|
| `IN_PROGRESS` | 审批进行中 |
| `APPROVED` | 全部节点审批通过 |
| `REJECTED` | （当前版本暂无写入路径，枚举值已定义） |

**`TaskStatus`** — 任务节点状态

| 枚举值 | 含义 |
|--------|------|
| `PENDING` | 等待审批人操作 |
| `DONE` | 已审批通过 |
| `BACKED` | 已被退回 |

**`ActionType`** — 操作历史动作类型

| 枚举值 | 含义 |
|--------|------|
| `START` | 发起审批流 |
| `APPROVE` | 审批通过 |
| `BACK` | 退回上一节点 |
| `TRANSFER` | 转交给其他人 |

### 模型类

**`ApprovalNode`** — 审批流节点定义（启动时传入）

| 字段 | 类型 | 说明 |
|------|------|------|
| `nodeId` | `String` | 节点标识 |
| `assignee` | `String` | 该节点的审批人 userId |

**`ApprovalInstance`** — 审批流实例

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 数据库自增主键 |
| `bizKey` | `String` | 业务唯一键，由调用方传入 |
| `status` | `ApprovalStatus` | 当前实例状态 |
| `currentStep` | `Integer` | 当前推进到第几个节点（从 0 开始） |
| `nodesJson` | `String` | 节点列表序列化为 JSON 存储 |
| `createdBy` | `String` | 发起人 userId |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 最后更新时间 |

**`ApprovalTask`** — 单个审批节点任务

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 数据库自增主键 |
| `instanceId` | `Long` | 所属审批实例 ID |
| `nodeId` | `String` | 对应节点标识 |
| `assignee` | `String` | 当前审批人 userId |
| `status` | `TaskStatus` | 任务状态 |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 最后更新时间 |

**`ApprovalHistory`** — 操作历史记录

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 数据库自增主键 |
| `instanceId` | `Long` | 所属审批实例 ID |
| `taskId` | `Long` | 关联任务 ID |
| `action` | `ActionType` | 操作类型 |
| `operator` | `String` | 操作人 userId |
| `comment` | `String` | 操作备注（可为 `null`） |
| `createdAt` | `LocalDateTime` | 操作时间 |

**`ApprovalDoneItem`** — 已处理条目（待办已办列表的响应元素）

| 字段 | 类型 | 说明 |
|------|------|------|
| `historyId` | `Long` | 对应历史记录 ID |
| `instanceId` | `Long` | 所属审批实例 ID |
| `taskId` | `Long` | 关联任务 ID |
| `nodeId` | `String` | 节点标识（关联 `approval_task` 获取） |
| `action` | `ActionType` | 操作类型 |
| `operator` | `String` | 操作人 userId |
| `comment` | `String` | 操作备注 |
| `createdAt` | `LocalDateTime` | 操作时间 |

---

## 持久化结构

表定义由 `ApprovalTables` 统一声明（jOOQ 风格，手写字段映射，无代码生成）。
数据库 schema 为 `approval`，包含三张表：

| jOOQ 常量 | 实际表名 |
|-----------|----------|
| `APPROVAL_INSTANCE` | `approval.approval_instance` |
| `APPROVAL_TASK` | `approval.approval_task` |
| `APPROVAL_HISTORY` | `approval.approval_history` |

`ApprovalRepository` 注入 `DSLContext`（由 `persistence` 模块提供），
实现对三张表的增删改查，枚举值以 `name()` 字符串存库、查询时用 `Enum.valueOf` 还原。

---

## 核心服务 API

### `ApprovalService`（`@ApplicationScoped`，受构建开关控制）

| 方法签名 | 说明 |
|----------|------|
| `long startProcess(String bizKey, List<ApprovalNode> nodes)` | 创建审批实例，生成第一个节点任务，写入 `START` 历史记录，返回 `instanceId` |
| `void approveTask(long taskId, String comment)` | 当前用户审批通过指定任务；若已是最后节点则将实例置为 `APPROVED`，否则创建下一节点任务 |
| `void backTask(long taskId, String comment)` | 当前用户将指定任务退回上一节点；第一节点不可退回 |
| `void transferTask(long taskId, String toUserId, String comment)` | 将指定任务转交给 `toUserId`，不推进节点步骤 |
| `List<ApprovalTask> listTodo()` | 查询当前用户所有状态为 `PENDING` 的待办任务 |
| `List<ApprovalDoneItem> listDone()` | 查询当前用户在历史记录中作为 operator 的所有已处理条目 |

所有写操作标注 `@Transactional`。操作人通过 `UserContext.getCurrentUserId()` 读取（见下文），
`approveTask` / `backTask` / `transferTask` 会校验 `task.assignee` 与当前用户是否一致，不一致时抛出 `IllegalStateException`。

### `ApprovalRepository`（`@ApplicationScoped`）

| 方法签名 | 说明 |
|----------|------|
| `long insertInstance(ApprovalInstance instance)` | 插入实例记录，返回生成的 `id` |
| `Optional<ApprovalInstance> fetchInstance(long instanceId)` | 按 ID 查询实例 |
| `void updateInstanceProgress(long instanceId, int currentStep, String status)` | 更新实例的当前步骤和状态 |
| `long insertTask(ApprovalTask task)` | 插入任务记录，返回生成的 `id` |
| `Optional<ApprovalTask> fetchTask(long taskId)` | 按 ID 查询任务 |
| `void updateTaskStatus(long taskId, TaskStatus status)` | 更新任务状态 |
| `void updateTaskAssignee(long taskId, String assignee)` | 更新任务审批人（转交使用） |
| `List<ApprovalTask> fetchTodoTasks(String assignee)` | 查询指定用户的所有 `PENDING` 任务，按 `created_at` 降序 |
| `List<ApprovalDoneItem> fetchDoneItems(String operator)` | 查询指定用户在历史记录中的所有条目，左关联 `approval_task` 补充 `nodeId`，按 `created_at` 降序 |
| `void insertHistory(ApprovalHistory history)` | 插入操作历史记录 |

---

## REST 接口

根路径：`/approval`，全部接口消费和生产 `application/json`。
调用方须在每个请求头中携带 `X-User-Id`，值为当前操作用户的 userId；缺失或空值返回 `400`。

### 发起审批流

```
POST /approval/process/start
Header: X-User-Id: {userId}
```

请求体 `ApprovalStartRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `bizKey` | `String` | 是 | 业务唯一键 |
| `nodes` | `List<ApprovalNode>` | 是（非空） | 审批节点列表，按顺序执行 |

每个 `ApprovalNode`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `nodeId` | `String` | 节点标识 |
| `assignee` | `String` | 审批人 userId |

响应体 `ApprovalStartResponse`（HTTP 200）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `instanceId` | `long` | 新建审批实例的 ID |

### 审批通过

```
POST /approval/task/{taskId}/approve
Header: X-User-Id: {userId}
```

请求体 `ApprovalActionRequest`（可选，可为空 body）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `comment` | `String` | 否 | 审批意见 |

响应：HTTP 200，无 body。

### 退回

```
POST /approval/task/{taskId}/back
Header: X-User-Id: {userId}
```

请求体同 `ApprovalActionRequest`（`comment` 可选）。  
响应：HTTP 200，无 body。

### 转交

```
POST /approval/task/{taskId}/transfer
Header: X-User-Id: {userId}
```

请求体 `ApprovalTransferRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `toUserId` | `String` | 是（不可为空） | 转交目标用户 userId |
| `comment` | `String` | 否 | 备注 |

响应：HTTP 200，无 body。`toUserId` 缺失或为空时返回 `400`。

### 查询待办

```
GET /approval/tasks/todo
Header: X-User-Id: {userId}
```

响应：HTTP 200，body 为 `List<ApprovalTask>`（当前用户所有 `PENDING` 任务，按创建时间降序）。

### 查询已处理

```
GET /approval/tasks/done
Header: X-User-Id: {userId}
```

响应：HTTP 200，body 为 `List<ApprovalDoneItem>`（当前用户在历史记录中的所有操作条目，按操作时间降序）。

---

## 用户上下文

`UserContext` 通过 `ThreadLocal<String>` 持有当前请求的操作用户 ID。
`ApprovalResource` 在每个方法入口调用 `UserContext.setCurrentUserId(userId)`，在 `finally` 块中调用 `UserContext.clear()`。

| 方法 | 说明 |
|------|------|
| `UserContext.setCurrentUserId(String userId)` | 设置当前线程的用户 ID |
| `UserContext.getCurrentUserId()` | 读取当前线程的用户 ID |
| `UserContext.clear()` | 清除 ThreadLocal（在 `finally` 块中调用） |

`UserDirectory` 提供一个内置的静态用户名册（当前硬编码了 `"carl"` 和 `"jack"` 两个条目），
暴露 `exists(String userId)` 和 `displayName(String userId)` 两个方法。
`ApprovalResource` 和 `ApprovalService` 本身不调用 `UserDirectory`，该类供扩展场景自行使用。

---

## 使用流程示例

以下示例使用 `curl`，`bizKey` 为 `order-001`，审批链为 `manager`（节点 `node-1`）→ `director`（节点 `node-2`）。

### 1. 发起审批流（发起人：`alice`）

```bash
curl -X POST http://localhost:8080/approval/process/start \
  -H "Content-Type: application/json" \
  -H "X-User-Id: alice" \
  -d '{
    "bizKey": "order-001",
    "nodes": [
      {"nodeId": "node-1", "assignee": "manager"},
      {"nodeId": "node-2", "assignee": "director"}
    ]
  }'
# 响应示例：{"instanceId": 1}
```

### 2. `manager` 查看待办

```bash
curl http://localhost:8080/approval/tasks/todo \
  -H "X-User-Id: manager"
# 响应示例：[{"id":1,"instanceId":1,"nodeId":"node-1","assignee":"manager","status":"PENDING",...}]
```

### 3. `manager` 审批通过

```bash
curl -X POST http://localhost:8080/approval/task/1/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: manager" \
  -d '{"comment": "同意"}'
# 响应：HTTP 200；系统自动为 director 创建 node-2 的待办任务
```

### 4. `manager` 将任务转交给其他人（可选）

若在审批前需换人，可先调用转交接口：

```bash
curl -X POST http://localhost:8080/approval/task/1/transfer \
  -H "Content-Type: application/json" \
  -H "X-User-Id: manager" \
  -d '{"toUserId": "deputy", "comment": "manager 出差，由 deputy 代审"}'
```

### 5. `director` 退回到上一节点

```bash
curl -X POST http://localhost:8080/approval/task/2/back \
  -H "Content-Type: application/json" \
  -H "X-User-Id: director" \
  -d '{"comment": "材料不完整，退回补充"}'
# 系统将 task-2 状态置为 BACKED，并为 manager 重新创建 node-1 的 PENDING 任务
```

### 6. 查询已处理记录

```bash
curl http://localhost:8080/approval/tasks/done \
  -H "X-User-Id: manager"
```

---

## 配置项

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `quarkus.plugins.approval.enable` | `String` | （未设置） | 构建时开关；设为 `"true"` 才激活 `ApprovalService` |

源码中无其他 `@ConfigProperty` 声明，模块不读取其他配置键。

---

## 注意事项

- **操作人校验**：`approveTask` / `backTask` / `transferTask` 均校验 `task.assignee == X-User-Id`，不一致时抛 `IllegalStateException`（HTTP 500）。如需在 REST 层统一转换错误码，需在应用侧配置 `ExceptionMapper`。
- **退回限制**：当前节点为第一步（`currentStep == 0`）时调用 `backTask` 会抛 `IllegalStateException("cannot back from first step")`。
- **节点推进策略**：审批流严格按 `nodes` 列表线性推进，不支持并行会签或条件分支。
- **`REJECTED` 状态**：`ApprovalStatus.REJECTED` 枚举值已定义，但当前版本的 `ApprovalService` 中无任何写入路径将实例置为 `REJECTED`。
- **构建开关作用范围**：`@IfBuildProperty` 只控制 `ApprovalService`；`ApprovalRepository`、`ApprovalTables`、`ApprovalResource` 等类无条件加载。若未开启开关，`ApprovalResource` 注入 `ApprovalService` 时会因 bean 缺失在启动时报错。
- **ThreadLocal 清理**：`UserContext` 使用 `ThreadLocal`，`ApprovalResource` 每个方法均在 `finally` 块中调用 `UserContext.clear()`，自定义调用链若绕过 `ApprovalResource` 直接调用 `ApprovalService`，需自行管理 `ThreadLocal` 的写入与清理。
- **数据库 DDL**：模块本身不提供 DDL 脚本，需在 `approval` schema 下手动建表（`approval_instance`、`approval_task`、`approval_history`），字段与 `ApprovalTables` 中的列定义对应。
