package org.carl.infrastructure.workflow.handlers;

import org.carl.infrastructure.workflow.spi.NodeSpec;
import org.carl.infrastructure.workflow.spi.NodeTypes;

/**
 * Strongly typed specs for built-in handlers.
 *
 * <p>Use these constants with {@code NodeBuilder.config(spec, config)} when Java call sites should
 * not spell config property keys.
 */
public final class BuiltInNodeSpecs {

    public static final NodeSpec<ServiceTaskConfig> SERVICE_TASK =
            NodeSpec.of(NodeTypes.SERVICE_TASK, ServiceTaskConfig.class);

    public static final NodeSpec<ApprovalTaskConfig> APPROVAL_TASK =
            NodeSpec.of(NodeTypes.APPROVAL_TASK, ApprovalTaskConfig.class);

    public static final NodeSpec<UserTaskConfig> USER_TASK =
            NodeSpec.of(NodeTypes.USER_TASK, UserTaskConfig.class);

    public static final NodeSpec<EventTaskConfig> EVENT_TASK =
            NodeSpec.of(NodeTypes.EVENT_TASK, EventTaskConfig.class);

    public static final NodeSpec<TimerTaskConfig> TIMER_TASK =
            NodeSpec.of(NodeTypes.TIMER_TASK, TimerTaskConfig.class);

    public static final NodeSpec<TaskGroupConfig> TASK_GROUP =
            NodeSpec.of(NodeTypes.TASK_GROUP, TaskGroupConfig.class);

    public static final NodeSpec<GatewayConfig> GATEWAY =
            NodeSpec.of(NodeTypes.GATEWAY, GatewayConfig.class);

    public static final NodeSpec<SubProcessConfig> SUB_PROCESS =
            NodeSpec.of(NodeTypes.SUB_PROCESS, SubProcessConfig.class);

    public static final NodeSpec<EndTaskConfig> END_TASK =
            NodeSpec.of(NodeTypes.END_TASK, EndTaskConfig.class);

    private BuiltInNodeSpecs() {
        throw new AssertionError("no instances");
    }
}
