package org.carl.infrastructure.workflow.example;

import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import org.carl.infrastructure.workflow.archive.DatabaseArchiveActivities;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.*;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;

import java.util.Map;
import java.util.UUID;

/**
 * 快速启动 Worker - 独立的 Worker 进程
 *
 * <p>运行这个类会启动一个 Temporal Worker，监听工作流任务
 *
 * <p>使用方法：
 * <pre>
 * # 在 IDEA 中右键运行，或者：
 * java org.carl.infrastructure.workflow.example.QuickStartWorker
 * </pre>
 */
public class QuickStartWorker {

    private static final String TEMPORAL_HOST = "180.184.66.147:31733";
    private static final String TASK_QUEUE = "QUICK_START_QUEUE";
    private static final String DB_URL = "jdbc:postgresql://180.184.66.147:31432/db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) {
        System.out.println("🚀 启动 Worker...");

        // 连接到 Temporal 服务器
        var service = WorkflowServiceStubs.newInstance(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(TEMPORAL_HOST)
                        .build());

        // 创建 Worker Factory
        var workerFactory = WorkerFactory.newInstance(
                io.temporal.client.WorkflowClient.newInstance(service));

        // 创建 Worker
        var worker = workerFactory.newWorker(TASK_QUEUE);

        // 注册内置处理器
        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        // 注册业务 Activities
        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();

        // 注册 "createLeaveRequest" activity
        activityRegistry.register("createLeaveRequest", input -> {
            String employeeId = input == null ? "unknown" : (String) input.get("employeeId");
            System.out.println("📝 创建请假请求: employeeId=" + employeeId);
            return Map.of(
                    "requestId", "REQ-" + UUID.randomUUID(),
                    "employeeId", employeeId,
                    "createdAt", java.time.Instant.now().toString()
            );
        });

        // 注册 "notifyManager" activity
        activityRegistry.register("notifyManager", input -> {
            System.out.println("🔔 通知主管: " + input);
            return Map.of("notified", true);
        });

        // 启用数据库归档
        DatabaseArchiveActivities archiveActivities = new DatabaseArchiveActivities(
                DB_URL, DB_USER, DB_PASSWORD);

        // 初始化 Worker
        WorkerSetup.setup(worker, handlerRegistry, activityRegistry, archiveActivities);

        // 启动 Worker
        workerFactory.start();

        System.out.println("✅ Worker 启动成功!");
        System.out.println("📡 监听任务队列: " + TASK_QUEUE);
        System.out.println("🔄 等待工作流...");
        System.out.println("💡 提示: 在另一个终端运行 QuickStartClient 来测试工作流");
        System.out.println("💡 访问 Temporal UI: http://" + TEMPORAL_HOST);

        // 保持运行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 关闭 Worker...");
            workerFactory.shutdown();
        }));
    }
}
