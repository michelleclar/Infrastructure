package org.carl.infrastructure.workflow.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import org.carl.infrastructure.workflow.archive.DatabaseArchiveActivities;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.*;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.Map;
import java.util.UUID;

/**
 * 快速启动示例 - 演示如何使用工作流引擎
 *
 * <p>这个示例展示了： 1. 如何启动 Worker 2. 如何定义工作流 3. 如何启动工作流实例 4. 如何发送信号
 */
public class QuickStartExample {

    private static final String TEMPORAL_HOST = "180.184.66.147:31733";
    private static final String TASK_QUEUE = "QUICK_START_QUEUE";
    private static final String DB_URL = "jdbc:postgresql://180.184.66.147:31432/db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: java QuickStartExample [worker|client]");
            System.out.println("  worker - 启动 Worker 进程");
            System.out.println("  client - 启动客户端并执行工作流");
            return;
        }

        switch (args[0]) {
            case "worker" -> startWorker();
            case "client" -> runClient();
            default -> {
                System.out.println("未知参数: " + args[0]);
            }
        }
    }

    /** 启动 Worker 进程 */
    private static void startWorker() {
        System.out.println("🚀 启动 Worker...");

        // 连接到 Temporal 服务器
        var service =
                WorkflowServiceStubs.newInstance(
                        WorkflowServiceStubsOptions.newBuilder().setTarget(TEMPORAL_HOST).build());

        // 创建 Worker Factory
        var workerFactory =
                WorkerFactory.newInstance(io.temporal.client.WorkflowClient.newInstance(service));

        // 创建 Worker
        var worker = workerFactory.newWorker(TASK_QUEUE);

        // 注册内置处理器
        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        // 注册业务 Activities
        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();

        // 注册 "createLeaveRequest" activity
        activityRegistry.register(
                "createLeaveRequest",
                input -> {
                    String employeeId =
                            input == null ? "unknown" : (String) input.get("employeeId");
                    System.out.println("📝 创建请假请求: employeeId=" + employeeId);
                    return Map.of(
                            "requestId",
                            "REQ-" + UUID.randomUUID(),
                            "employeeId",
                            employeeId,
                            "createdAt",
                            java.time.Instant.now().toString());
                });

        // 注册 "notifyManager" activity
        activityRegistry.register(
                "notifyManager",
                input -> {
                    System.out.println("🔔 通知主管: " + input);
                    return Map.of("notified", true);
                });

        // 启用数据库归档
        DatabaseArchiveActivities archiveActivities =
                new DatabaseArchiveActivities(DB_URL, DB_USER, DB_PASSWORD);

        // 初始化 Worker
        WorkerSetup.setup(worker, handlerRegistry, activityRegistry, archiveActivities);

        // 启动 Worker
        workerFactory.start();

        System.out.println("✅ Worker 启动成功!");
        System.out.println("📡 监听任务队列: " + TASK_QUEUE);
        System.out.println("🔄 等待工作流...");
    }

    /** 启动客户端并执行工作流 */
    private static void runClient() throws Exception {
        System.out.println("🎯 启动客户端...");

        // 连接到 Temporal
        var service =
                WorkflowServiceStubs.newInstance(
                        WorkflowServiceStubsOptions.newBuilder().setTarget(TEMPORAL_HOST).build());

        var client = io.temporal.client.WorkflowClient.newInstance(service);

        // 创建工作流定义
        WorkflowDefinition workflow = createSimpleWorkflow();

        // 创建工作流输入
        var mapper = new ObjectMapper();
        var businessData =
                mapper.createObjectNode()
                        .put("employee", "alice")
                        .put("manager", "bob")
                        .put("days", 3);

        var input = new WorkflowInput(workflow, businessData, Map.of(), null, Boolean.TRUE);

        // 启动工作流
        String workflowId = "quick-start-" + UUID.randomUUID();
        var workflowStub =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId(workflowId)
                                .build());

        // 异步启动
        io.temporal.client.WorkflowClient.start(workflowStub::execute, input);

        System.out.println("✅ 工作流启动成功!");
        System.out.println("📋 工作流ID: " + workflowId);
        System.out.println("⏳ 等待 2 秒...");

        // 等待工作流到达审批节点
        Thread.sleep(2000);

        // 发送审批信号
        System.out.println("👍 发送审批信号...");
        var payload = JsonNodeFactory.instance.objectNode().put("decision", "approved");
        workflowStub.signal(new WorkflowEvent("approval", payload));

        System.out.println("✅ 审批完成!");
        System.out.println("💡 提示: 可以在 Temporal UI 查看: http://" + TEMPORAL_HOST);
    }

    /** 创建一个简单的请假工作流 - 使用简洁的 FlowDef DSL */
    private static WorkflowDefinition createSimpleWorkflow() {
        // 使用 FlowDef DSL（简洁语法）
        FlowDef flow = Flow.define("quickStart", "快速示例");
        flow.start("requestLeave");
        flow.node("requestLeave", BuiltInNodes.service("createLeaveRequest"));
        flow.node("managerApproval", BuiltInNodes.approval("manager"));
        flow.node("notifyManager", BuiltInNodes.service("notifyManager"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("已完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("已拒绝"));

        // 定义路由
        flow.from("requestLeave").on("success").to("managerApproval");
        flow.from("managerApproval").on("approved").to("notifyManager");
        flow.from("managerApproval").on("rejected").to("rejected");
        flow.from("notifyManager").on("success").to("completed");

        return flow.build();
    }
}
