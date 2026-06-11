package org.carl.infrastructure.workflow.example;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.Map;
import java.util.UUID;

/**
 * 快速启动客户端 - 独立的客户端进程
 *
 * <p>运行这个类会启动一个工作流实例并发送审批信号
 *
 * <p>使用方法：
 * <pre>
 * # 在 IDEA 中右键运行（确保 Worker 已启动），或者：
 * java org.carl.infrastructure.workflow.example.QuickStartClient
 * </pre>
 */
public class QuickStartClient {

    private static final String TEMPORAL_HOST = "180.184.66.147:31733";
    private static final String TASK_QUEUE = "QUICK_START_QUEUE";

    public static void main(String[] args) throws Exception {
        System.out.println("🎯 启动客户端...");

        // 连接到 Temporal
        var service = WorkflowServiceStubs.newInstance(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(TEMPORAL_HOST)
                        .build());

        var client = WorkflowClient.newInstance(service);

        // 创建工作流定义
        WorkflowDefinition workflow = createSimpleWorkflow();

        // 创建工作流输入
        var input = WorkflowInput.from(
                workflow,
                Map.of("employee", "alice", "manager", "bob", "days", 3)
        );

        // 启动工作流
        String workflowId = "quick-start-" + UUID.randomUUID();
        var workflowStub = client.newWorkflowStub(
                GenericWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build()
        );

        // 异步启动
        WorkflowClient.start(workflowStub::execute, input);

        System.out.println("✅ 工作流启动成功!");
        System.out.println("📋 工作流ID: " + workflowId);
        System.out.println("⏳ 等待 2 秒...");

        // 等待工作流到达审批节点
        Thread.sleep(2000);

        // 发送审批信号
        System.out.println("👍 发送审批信号...");
        var payload = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                .put("decision", "approved");
        workflowStub.signal(new WorkflowEvent("approval", payload));

        System.out.println("✅ 审批完成!");
        System.out.println("💡 提示: 可以在 Temporal UI 查看详细信息: http://" + TEMPORAL_HOST);

        System.out.println("💡 提示: 可以在 Temporal UI 查看详细信息: http://" + TEMPORAL_HOST);
    }

    /**
     * 创建一个简单的请假工作流 - 使用简洁的 FlowDef DSL
     */
    private static WorkflowDefinition createSimpleWorkflow() {
        FlowDef flow = Flow.define("quickStart", "快速示例");
        flow.start("requestLeave");

        // activityInput 是字面 Map（ServiceTaskHandler 不做占位符替换），需要动态值
        // 时在自定义 NodeHandler 内读 ctx.businessData() / ctx.resultOf(...) 拼。
        flow.node("requestLeave", BuiltInNodes.service("createLeaveRequest")
                .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));

        flow.node("managerApproval", BuiltInNodes.approval("bob"));

        flow.node("notifyManager", BuiltInNodes.service("notifyManager"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("已完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("已拒绝"));

        // 路由：event 名必须用 Outcomes 常量（大写敏感）；用字面 "success"/"approved"
        // 会和 ServiceTask/ApprovalTask 返回的 Outcomes.SUCCESS/APPROVED 匹配不上，
        // pickNextEdge 返回 null → workflow 在当前节点直接终止。
        flow.from("requestLeave").on(Outcomes.SUCCESS).to("managerApproval");
        flow.from("managerApproval").on(Outcomes.APPROVED).to("notifyManager");
        flow.from("managerApproval").on(Outcomes.REJECTED).to("rejected");
        flow.from("notifyManager").on(Outcomes.SUCCESS).to("completed");

        return flow.build();
    }
}
