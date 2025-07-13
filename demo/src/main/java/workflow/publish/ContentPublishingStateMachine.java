package workflow.publish;

import org.carl.infrastructure.statemachine.Action;
import org.carl.infrastructure.statemachine.Condition;
import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.statemachine.builder.StateMachineBuilder;
import org.carl.infrastructure.statemachine.builder.StateMachineBuilderFactory;

import workflow.model.ContentPublishingContext;
import workflow.model.ContentPublishingEventEnum;
import workflow.model.ContentPublishingStatusEnum;

public class ContentPublishingStateMachine {
    public static final String machineId = "content-publishing-workflow";

    private static Condition<ContentPublishingContext> checkCensorIsAgree() {
        return context -> {
            System.out.println("Check approval condition : " + context);
            // 审核通过的条件：
            // 1. 内容已准备好
            // 2. 有审核员ID
            // 3. 没有拒绝原因（表示审核通过）
            return context.isContentReady()
                    && context.getReviewerId() != null
                    && context.getRejectReason() == null;
        };
    }

    public static StateMachine<
                    ContentPublishingStatusEnum,
                    ContentPublishingEventEnum,
                    ContentPublishingContext>
            STATEMACHINE;

    static {
        StateMachineBuilder<
                        ContentPublishingStatusEnum,
                        ContentPublishingEventEnum,
                        ContentPublishingContext>
                builder = StateMachineBuilderFactory.create();
        externalTransition(builder);
        STATEMACHINE = builder.build(machineId);
    }

    private static Action<
                    ContentPublishingStatusEnum,
                    ContentPublishingEventEnum,
                    ContentPublishingContext>
            doAction() {
        return (from, to, event, ctx) -> {
            System.out.println("from: " + from + " to: " + to + " ctx: " + ctx);
        };
    }

    private static Condition<ContentPublishingContext> checkRejectCondition() {
        return context -> {
            System.out.println("Check reject condition : " + context);
            // 审核拒绝的条件：
            // 1. 有审核员ID
            // 2. 有拒绝原因
            return context.getReviewerId() != null
                    && context.getRejectReason() != null
                    && !context.getRejectReason().trim().isEmpty();
        };
    }

    private static Condition<ContentPublishingContext> checkWithdrawCondition() {
        return context -> {
            System.out.println("Check withdraw condition : " + context);
            // 撤回条件：作者本人或管理员可以撤回被拒绝的内容
            return context.canWithdraw();
        };
    }

    private static Condition<ContentPublishingContext> checkReviseCondition() {
        return context -> {
            System.out.println("Check revise condition : " + context);
            // 修改重提条件：作者本人可以修改被拒绝的内容
            return context.canRevise();
        };
    }

    private static Condition<ContentPublishingContext> checkPublishCondition() {
        return context -> {
            System.out.println("Check publish condition : " + context);
            // 发布条件：内容已准备好且没有拒绝原因
            return context.canPublish();
        };
    }

    private static Condition<ContentPublishingContext> checkUnpublishCondition() {
        return context -> {
            System.out.println("Check unpublish condition : " + context);
            // 取消发布条件：管理员或作者可以取消发布
            return context.canUnpublish();
        };
    }

    private static Condition<ContentPublishingContext> checkRepublishCondition() {
        return context -> {
            System.out.println("Check republish condition : " + context);
            // 重新发布条件：内容已准备好且有发布权限
            return context.canPublish() && context.canUnpublish();
        };
    }

    private boolean doCensor(String content) {
        return "abc".equals(content);
    }

    private static Condition<ContentPublishingContext> checkIsReady() {
        return context -> {
            System.out.println("Check is ready : " + context);
            return context.isContentReady();
        };
    }

    public String machineId() {
        return "content-publishing-workflow";
    }

    public static void externalTransition(
            StateMachineBuilder<
                            ContentPublishingStatusEnum,
                            ContentPublishingEventEnum,
                            ContentPublishingContext>
                    stateMachine) {
        // 草稿 提交 提交审核
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.DRAFT)
                .to(ContentPublishingStatusEnum.SUBMITTED)
                .on(ContentPublishingEventEnum.SUBMIT)
                .when(checkIsReady())
                .perform(doAction());
        // 提交审核 审核通过  审核通过进行格式转换
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.SUBMITTED)
                .to(ContentPublishingStatusEnum.PROCESSING)
                .on(ContentPublishingEventEnum.APPROVE)
                .when(checkCensorIsAgree())
                .perform(doAction());
        // 提交审核 审核拒绝 已经拒绝
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.SUBMITTED)
                .to(ContentPublishingStatusEnum.REJECTED)
                .on(ContentPublishingEventEnum.REJECT)
                .when(checkRejectCondition())
                .perform(doAction());
        // 已经拒绝 撤销拒绝 提交审核
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.REJECTED)
                .to(ContentPublishingStatusEnum.SUBMITTED)
                .on(ContentPublishingEventEnum.WITHDRAW)
                .when(checkWithdrawCondition())
                .perform(doAction());

        // 已经拒绝 修改重提 草稿状态
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.REJECTED)
                .to(ContentPublishingStatusEnum.DRAFT)
                .on(ContentPublishingEventEnum.REVISE)
                .when(checkReviseCondition())
                .perform(doAction());

        // 审核通过，进行格式转换和优化 发布 已经发布
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.PROCESSING)
                .to(ContentPublishingStatusEnum.PUBLISHED)
                .on(ContentPublishingEventEnum.PUBLISH)
                .when(checkPublishCondition())
                .perform(doAction());
        // 已经发布 取消发布  未发布
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.PUBLISHED)
                .to(ContentPublishingStatusEnum.UNPUBLISHED)
                .on(ContentPublishingEventEnum.UNPUBLISH)
                .when(checkUnpublishCondition())
                .perform(doAction());

        // 未发布 发布  已经发布
        stateMachine
                .externalTransition()
                .from(ContentPublishingStatusEnum.UNPUBLISHED)
                .to(ContentPublishingStatusEnum.PUBLISHED)
                .on(ContentPublishingEventEnum.PUBLISH)
                .when(checkRepublishCondition())
                .perform(doAction());
    }
}
