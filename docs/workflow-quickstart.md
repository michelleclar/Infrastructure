# 工作流引擎快速开始

## 🚀 一分钟体验

### 第一步：启动 Worker

**方式1：在 IDEA 中运行**
- 打开 `infrastructure-component-workflow-temporal/src/test/java/org/carl/infrastructure/workflow/example/QuickStartWorker.java`
- 右键点击，选择 "Run 'QuickStartWorker.main()'"

**方式2：使用 Gradle**
```bash
./gradlew :infrastructure-component-workflow-temporal:test --tests QuickStartWorker
```

Worker 会启动并监听工作流任务，保持运行状态。

### 第二步：启动客户端

**方式1：在 IDEA 中运行**
- 打开 `infrastructure-component-workflow-temporal/src/test/java/org/carl/infrastructure/workflow/example/QuickStartClient.java`
- 右键点击，选择 "Run 'QuickStartClient.main()'"

**方式2：使用 Gradle**
```bash
./gradlew :infrastructure-component-workflow-temporal:test --tests QuickStartClient
```

客户端会自动：
1. 创建并启动工作流实例
2. 发送审批信号
3. 显示执行结果

## 📊 查看执行状态

### Temporal UI
访问 http://180.184.66.147:31733 查看工作流执行历史和节点状态。

### 数据库
```sql
SELECT workflow_id, status, final_node_id, started_at 
FROM workflow_instance 
WHERE definition_id = 'quickStart' 
ORDER BY started_at DESC;
```

## 📱 实际使用场景

### 场景1：请假审批流程

```java
import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.*;
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

// 定义工作流（简洁语法）
FlowDef flow = Flow.define("leave", "请假流程");
flow.start("发起请假");
flow.node("发起请假", service("createLeaveRequest", Map.of("employeeId", "alice")));
flow.node("经理审批", approval("manager"));
flow.node("通知主管", service("notifyManager", Map.of()));
flow.endNode("已批准");
flow.endNode("已拒绝");

// 定义路由（on 语法）
flow.from("发起请假").on("success").to("经理审批");
flow.from("经理审批").on("approved").to("通知主管");
flow.from("经理审批").on("rejected").to("已拒绝");
flow.from("通知主管").on("success").to("已批准");

WorkflowDefinition leaveWorkflow = flow.build();
```

### 场景2：订单处理流程

```java
import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.*;
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

FlowDef flow = Flow.define("order", "订单流程");
flow.start("createOrder");
flow.node("createOrder", service("createOrder", Map.of()));
flow.node("checkStock", service("checkStock", Map.of()));
flow.node("payment", service("processPayment", Map.of()));
flow.node("shipping", service("shipOrder", Map.of()));
flow.endNode("订单完成");
flow.endNode("订单失败");

flow.from("createOrder").on("success").to("checkStock");
flow.from("checkStock").on("success").to("payment");
flow.from("payment").on("success").to("shipping");
flow.from("shipping").on("success").to("订单完成");
flow.from("payment").on("failed").to("订单失败");

WorkflowDefinition orderWorkflow = flow.build();
```

### 场景3：会签流程

```java
import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.*;
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

FlowDef flow = Flow.define("approval", "会签流程");
flow.start("submitRequest");

// 定义节点（简洁语法）
flow.node("submitRequest", service("submitRequest", Map.of()));
flow.endNode("approved");
flow.endNode("rejected");

// 定义路由（on语法更直观）
flow.from("submitRequest").on("success").to("groupApproval");
flow.from("groupApproval")
    .join(all(
        node("hrApproval", approval("hr")),
        node("managerApproval", approval("manager"))))
    .on("approved").to("approved")
    .on("rejected").to("rejected");

WorkflowDefinition approvalWorkflow = flow.build();
```

## 🔥 核心功能

### 1. 节点类型

- **SERVICE_TASK**: 执行业务逻辑（如调用API、数据库操作）
- **APPROVAL_TASK**: 等待审批，支持超时
- **TASK_GROUP**: 多人会签，支持表达式（AND/OR）
- **END_TASK**: 结束节点

### 2. 路由方式

- **按结果路由**: `.on("success").to("nextNode")`
- **按条件路由**: `.on("success").when("${amount > 1000}").to("highValueApprove")`
- **回边路由**: 支持循环和重试

### 3. 上下文管理

```java
// 启动数据 (只读)
String employee = businessData.get("employee");

// 变量 (可读写)
workflow.setVariable("approvalCount", 1);

// 节点结果 (只读历史)
NodeResult result = workflow.resultOf("requestLeave");
String requestId = result.payload().get("requestId");
```

## 🎯 快速示例代码

### 1. 定义业务 Activity

```java
BusinessActivityRegistry registry = new BusinessActivityRegistry();

registry.register("calculateSalary", input -> {
    String employeeId = (String) input.get("employeeId");
    int days = (Integer) input.get("days");
    
    // 调用薪资计算服务
    double salary = salaryService.calculate(employeeId, days);
    
    return Map.of("salary", salary, "calculatedAt", Instant.now());
});
```

### 2. 注册 Activity 到 Worker

```java
WorkerSetup.setup(worker, handlerRegistry, activityRegistry);
```

### 3. 启动工作流

```java
WorkflowInput input = new WorkflowInput(
    workflowJson,
    businessData,  // {"employee": "alice", "days": 3}
    Map.of(),       // 变量
    "ORDER-123"     // 业务键
);

WorkflowClient.start(workflow::execute, input);
```

## 💡 最佳实践

### 1. Activity 设计

```java
// ✅ 好的做法 - 快速执行
registry.register("sendNotification", input -> {
    notificationService.send(input);
    return Map.of("sent", true);
});

// ❌ 不好的做法 - 阻塞操作
registry.register("syncExternalApi", input -> {
    // 避免在 Activity 中进行长时间的阻塞调用
    return result;
});
```

### 2. 错误处理

```java
registry.register("riskyOperation", input -> {
    try {
        return riskyOperation(input);
    } catch (Exception e) {
        // 返回错误信息而不是抛出异常
        return Map.of("success", false, "error", e.getMessage());
    }
});
```

### 3. 超时设置

```java
import org.carl.infrastructure.workflow.handlers.ApprovalTaskConfig;
import org.carl.infrastructure.workflow.handlers.BuiltInNodeSpecs;

flow.node("approval", b -> b.config(
    BuiltInNodeSpecs.APPROVAL_TASK,
    new ApprovalTaskConfig("manager", "approval", "PT24H"))); // 24小时超时
```

## 📊 监控和调试

### Temporal UI
访问 `http://180.184.66.147:31733` 查看：
- 工作流执行历史
- 节点执行时间线
- 变量变化过程
- 错误堆栈信息

### 数据库查询

```sql
-- 查看最近的工作流
SELECT workflow_id, definition_id, status, 
       started_at, ended_at, final_node_id 
FROM workflow_instance 
WHERE started_at > NOW() - INTERVAL '1 day'
ORDER BY started_at DESC;

-- 查看特定工作流的执行记录
SELECT node_id, visit_no, outcome, status, 
       executed_at, result_json 
FROM execution_record 
WHERE workflow_id = 'your-workflow-id'
ORDER BY executed_at;
```

## 🎉 现在开始体验吧！

1. 启动 Worker 进程监听任务
2. 启动客户端提交工作流
3. 在 Temporal UI 中查看执行过程
4. 发送信号推进工作流

祝你使用愉快！🚀
