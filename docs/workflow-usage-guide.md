# 工作流引擎使用指南

## 🚀 快速开始

### 1. 环境准备

**开发环境：**
- PostgreSQL: `jdbc:postgresql://180.184.66.147:31432/db` (用户: root)
- Temporal: `180.184.66.147:31733`

### 2. 启动 Temporal Worker

```java
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;

public class WorkflowServer {

    public static void main(String[] args) {
        // 连接到 Temporal 服务器
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget("180.184.66.147:31733")
                .build()
        );

        // 创建 Worker Factory
        WorkerFactory workerFactory = WorkerFactory.newInstance(
            io.temporal.client.WorkflowClient.newInstance(service)
        );

        // 创建 Worker
        Worker worker = workerFactory.newWorker("WORKFLOW_TASK_QUEUE");

        // 注册内置处理器
        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        // 注册业务 Activities
        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();
        activityRegistry.register("approveLeave", input -> {
            // 审批请假逻辑
            String employeeId = (String) input.get("employeeId");
            int days = (Integer) input.get("days");
            // ... 业务逻辑 ...
            return Map.of("approved", true, "approver", "manager");
        });

        // 初始化 Worker
        WorkerSetup.setup(worker, handlerRegistry, activityRegistry);

        // 启动 Worker
        workerFactory.start();
        System.out.println("Worker started, waiting for workflows...");
    }
}
```

### 3. 定义工作流

使用简洁的 FlowDef DSL 语法：

```java
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

// 定义请假流程
FlowDef flow = Flow.define("leave", "请假流程");
flow.start("requestLeave");

// 定义节点
flow.node("requestLeave", service("createLeaveRequest", Map.of("employeeId", "${employee}")));
flow.node("managerApproval", approval(Map.of(
    "assignee", "${manager}",
    "awaitEvent", "approval",
    "timeoutDuration", "PT24H"
)));
flow.node("hrApproval", approval(Map.of(
    "assignee", "hr",
    "awaitEvent", "approval",
    "timeoutDuration", "PT24H"
)));
flow.node("onLeave", service("notifyManager", Map.of()));
flow.endNode("approved", "已批准");
flow.endNode("rejected", "已拒绝");

// 定义路由
flow.from("requestLeave").on("success").to("managerApproval");
flow.from("managerApproval").on("approved").to("hrApproval");
flow.from("managerApproval").on("rejected").to("rejected");
flow.from("hrApproval").on("approved").to("onLeave");
flow.from("hrApproval").on("rejected").to("rejected");
flow.from("onLeave").on("success").to("approved");

WorkflowDefinition leaveWorkflow = flow.build();
```

### 4. 启动工作流

```java
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WorkflowClient {

    public static void main(String[] args) throws Exception {
        // 连接到 Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget("180.184.66.147:31733")
                .build()
        );

        WorkflowClient client = WorkflowClient.newInstance(service);

        // 创建工作流输入
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode businessData = mapper.createObjectNode()
            .put("employee", "alice")
            .put("manager", "bob")
            .put("days", 3);

        String workflowJson = mapper.writeValueAsString(leaveWorkflow);

        WorkflowInput input = new WorkflowInput(
            workflowJson,
            businessData,
            Map.of(),  // 变量
            null        // 业务键
        );

        // 启动工作流
        GenericWorkflow workflow = client.newWorkflowStub(
            GenericWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("WORKFLOW_TASK_QUEUE")
                .setWorkflowId("leave-" + UUID.randomUUID())
                .build()
        );

        // 异步启动
        WorkflowClient.start(workflow::execute, input);
        System.out.println("Workflow started!");
    }

    // 发送审批信号
    public static void approveLeave(String workflowId, boolean approved) {
        workflow.signal(new WorkflowEvent("approval", 
            Map.of("decision", approved ? "approved" : "rejected")
        ));
    }
}
```

## 📚 核心概念

### 1. 节点类型

#### **SERVICE_TASK** - 服务任务
执行业务逻辑，返回固定结果。

```java
flow.node("calculateSalary", service("calculateSalary", Map.of()));
```

#### **APPROVAL_TASK** - 审批任务
等待外部信号，支持超时。

```java
flow.node("managerApproval", approval(Map.of(
    "assignee", "manager",
    "awaitEvent", "approval",
    "timeoutDuration", "PT24H"
)));
```

#### **END_TASK** - 结束节点
终止工作流。

```java
flow.endNode("completed", "已完成");
```

### 2. 边缘路由

工作流通过 `outcome` 和可选的 `condition` 路由：

```java
// 按结果路由
flow.from("approval").on("approved").to("nextStep");

// 按条件路由（新增 .when() 语法）
flow.from("checkAmount")
    .on(Outcomes.SUCCESS)
    .when("${amount} > 10000}")  // 条件为真时路由到 ceoApproval
    .to("ceoApproval");

flow.from("checkAmount")
    .on(Outcomes.SUCCESS)
    .to("managerApproval");  // 默认路由（无条件）
```

### 3. 上下文变量

**三层上下文模型：**

```java
// businessData - 启动时传入，不可变
Map<String, Object> businessData = Map.of("employee", "alice");

// variables - 可写，用于节点间传递数据
workflow.setVariable("approvalCount", 1);

// results.nodeId.* - 只读历史记录
NodeResult requestResult = workflow.resultOf("requestLeave");
String requestId = requestResult.payload().get("requestId");
```

## 🎯 实战例子

### 完整的请假流程

```java
import static org.carl.infrastructure.workflow.dsl.Dsl.*;

public class LeaveWorkflowExample {

    public static WorkflowDefinition createLeaveWorkflow() {
        FlowDef flow = Flow.define("leave", "请假流程");
        flow.start("submitLeave");

        // 提交请假
        flow.node("submitLeave", service("createLeaveRequest", Map.of()));

        // 经理审批
        flow.node("managerApproval", approval(Map.of(
            "assignee", "${manager}",
            "awaitEvent", "approval",
            "timeoutDuration", "PT24H"
        )));

        // HR 复核（大额需要）
        flow.node("hrApproval", approval(Map.of(
            "assignee", "hr",
            "awaitEvent", "approval",
            "timeoutDuration", "PT24H"
        )));

        // 通知主管
        flow.node("notifyManager", service("notifyManager", Map.of()));

        // 结束节点
        flow.endNode("approved", "已批准");
        flow.endNode("rejected", "已拒绝");
        flow.endNode("timeout", "已超时");

        // 路由规则
        flow.from("submitLeave").on("success").to("managerApproval");
        flow.from("managerApproval").on("approved").to("hrApproval");
        flow.from("managerApproval").on("rejected").to("rejected");
        flow.from("managerApproval").on("timeout").to("timeout");
        flow.from("hrApproval").on("approved").to("notifyManager");
        flow.from("hrApproval").on("rejected").to("rejected");
        flow.from("notifyManager").on("success").to("approved");

        return flow.build();
    }

    public static void main(String[] args) throws Exception {
        // 1. 启动 Worker
        startWorker();
        
        // 2. 启动工作流
        startWorkflow();
        
        // 3. 模拟审批
        Thread.sleep(2000);
        approveWorkflow("workflow-id", true);
    }

    static void startWorker() {
        // ... Worker 启动代码 ...
    }

    static void startWorkflow() throws Exception {
        // ... 工作流启动代码 ...
    }

    static void approveWorkflow(String workflowId, boolean approved) {
        // ... 发送审批信号 ...
    }
}
```

## 🔧 高级功能

### 1. 会签（TaskGroup）

使用 `join()` 和 `all()` 或 `any()` 定义多人会签：

```java
// 全会签（AND）- 所有人都需要批准
flow.from("requestLeave")
    .on(Outcomes.SUCCESS)
    .join(all(
        node("hrApproval", approval(Map.of("assignee", "hr"))),
        node("managerApproval", approval(Map.of("assignee", "manager")))
    ))
    .on(Outcomes.APPROVED).to("onLeave")
    .on(Outcomes.REJECTED).to("rejected");

// 或签（ANY）- 任一人批准即可
flow.from("requestLeave")
    .on(Outcomes.SUCCESS)
    .join(any(
        node("hrApproval", approval(Map.of("assignee", "hr"))),
        node("managerApproval", approval(Map.of("assignee", "manager")))
    ))
    .on(Outcomes.APPROVED).to("onLeave")
    .on(Outcomes.REJECTED).to("rejected");
```

### 2. 条件路由

使用 `.when()` 添加条件表达式：

```java
flow.node("checkAmount", service("getLeaveDays", Map.of()));
flow.endNode("approved", "已批准");
flow.endNode("rejected", "已拒绝");

// 带条件的路由（条件为真时执行）
flow.from("checkAmount")
    .on(Outcomes.SUCCESS)
    .when("${days} > 3}")  // 大于3天需要经理审批
    .to("managerApproval");

// 默认路由（无条件，fallback）
flow.from("checkAmount")
    .on(Outcomes.SUCCESS)
    .to("autoApprove");  // 小于等于3天自动批准
```

### 3. 补偿（Saga）

```java
flow.node("bookFlight", service("bookFlight",
    Map.of("activity", "bookFlight", "compensateActivity", "cancelFlight")));

flow.node("bookHotel", service("bookHotel",
    Map.of("activity", "bookHotel", "compensateActivity", "cancelHotel")));

// 如果后续节点失败，自动按相反顺序执行补偿
```

## 📊 监控和归档

### 启用数据库归档

```java
// 启用归档功能
DatabaseArchiveActivities archiveActivities = new DatabaseArchiveActivities(
    "jdbc:postgresql://180.184.66.147:31432/db",
    "root",
    "root"
);

WorkerSetup.setup(worker, handlerRegistry, activityRegistry, archiveActivities);
```

### 查询工作流状态

```sql
-- 查看工作流实例
SELECT workflow_id, status, final_node_id, started_at, ended_at 
FROM workflow_instance 
WHERE definition_id = 'leave' 
ORDER BY started_at DESC;

-- 查看执行记录
SELECT node_id, visit_no, outcome, status, executed_at 
FROM execution_record 
WHERE workflow_id = 'your-workflow-id' 
ORDER BY executed_at;
```

## 🐛 调试技巧

### 1. 本地测试环境

```java
// 使用 Temporal 测试环境
TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
Worker worker = testEnv.newWorker("TEST_QUEUE");
// ... 设置 Worker ...
testEnv.start();

// 模拟时间推进
testEnv.sleep(Duration.ofHours(25));  // 模拟超时
```

### 2. 日志配置

在 `src/test/resources/logging.properties`:

```properties
handlers=java.util.logging.ConsoleHandler
.level=INFO
org.carl.infrastructure.workflow.level=FINEST
io.temporal.level=WARNING
```

### 3. 查看 Temporal UI

访问 `http://180.184.66.147:31733` 查看：
- 工作流执行历史
- 节点执行状态
- 变量变化轨迹

## 🚀 下一步

1. **定义业务 Activities**：实现你的业务逻辑
2. **设计工作流 DSL**：使用 DSL 定义业务流程
3. **配置 Worker**：注册 Activities 和处理器
4. **启动工作流**：通过 Client 启动实例
5. **监控执行**：通过 UI 或数据库查询状态

祝你使用愉快！🎉
