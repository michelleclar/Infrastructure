package org.carl.test;

import org.carl.infrastructure.statemachine.Action;
import org.carl.infrastructure.statemachine.Condition;
import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.statemachine.builder.StateMachineBuilder;
import org.carl.infrastructure.statemachine.builder.StateMachineBuilderFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;

public class ContentPublishingStateMachineTest {
    static String MACHINE_ID = "CONTENT_PUBLISHING_STATE_MACHINE";

    enum ContentPublishingStatusEnum {
        /** 草稿状态，内容创建和编辑阶段 */
        DRAFT,
        /** 提交审核，等待审核员处理 */
        SUBMITTED,
        /** 审核通过，进行格式转换和优化 */
        PROCESSING,
        /** 已发布，内容对外可见 */
        PUBLISHED,
        /** 已拒绝，需要修改后重新提交 */
        REJECTED,
        UNPUBLISHED,
    }

    enum ContentPublishingEventEnum {
        /** 提交审核 */
        SUBMIT,
        /** 审核通过 */
        APPROVE,
        /** 审核拒绝 */
        REJECT,
        /** 发布内容 */
        PUBLISH,
        /** 修改重提 */
        REVISE,
        /** 撤回内容 */
        WITHDRAW,
        /** 取消发布 */
        UNPUBLISH,
    }

    static class ContentPublishingContext {
        /** 业务主键id */
        String entityId;

        String auth;
        String content;
        String authorId;
        String reviewerId;
        String rejectReason;
        List<String> tags;
        Timestamp submitTime;
        Timestamp publishTime;
        String revisionCount;

        /**
         * 内容准备检查
         *
         * @return boolean
         */
        public boolean isContentReady() {
            return content != null && !content.isEmpty();
        }

        // 审核条件检查
        public boolean isApprovalConditionMet() {
            return authorId != null && rejectReason == null;
        }

        // 是否已发布
        public boolean isPublished() {
            // 是否已发布
            return publishTime != null;
        }

        // 是否已下架
        public boolean isUnpublished() {
            return publishTime == null;
        }

        // 是否可以撤回（作者本人或管理员）
        public boolean canWithdraw() {
            return auth != null && (auth.equals(authorId) || "admin".equals(auth));
        }

        // 是否可以修改重提（作者本人）
        public boolean canRevise() {
            return auth != null && auth.equals(authorId);
        }

        // 是否可以发布（内容已处理完成）
        public boolean canPublish() {
            return isContentReady() && rejectReason == null;
        }

        // 是否可以取消发布（管理员或作者）
        public boolean canUnpublish() {
            return auth != null && (auth.equals(authorId) || "admin".equals(auth));
        }

        // 标记为已发布
        public void markAsPublished(String by) {
            this.publishTime = new Timestamp(System.currentTimeMillis());
        }

        // 标记为下架
        public void markAsUnpublished(String by, String reason) {
            this.publishTime = null;
            this.rejectReason = reason;
        }

        @Override
        public String toString() {
            return "{"
                    + "        \"entityId\":\""
                    + entityId
                    + "\""
                    + ",         \"auth\":\""
                    + auth
                    + "\""
                    + ",         \"content\":\""
                    + content
                    + "\""
                    + ",         \"authorId\":\""
                    + authorId
                    + "\""
                    + ",         \"reviewerId\":\""
                    + reviewerId
                    + "\""
                    + ",         \"rejectReason\":\""
                    + rejectReason
                    + "\""
                    + ",         \"tags\":"
                    + tags
                    + ",         \"submitTime\":"
                    + submitTime
                    + ",         \"publishTime\":"
                    + publishTime
                    + ",         \"revisionCount\":\""
                    + revisionCount
                    + "\""
                    + "}";
        }

        public static ContentPublishingContext VIOLATION() {
            ContentPublishingContext contentPublishingContext = new ContentPublishingContext();
            contentPublishingContext.entityId = "123456";
            contentPublishingContext.auth = "admin";
            contentPublishingContext.content = "abc";
            contentPublishingContext.authorId = "admin";
            contentPublishingContext.reviewerId = "123";
            contentPublishingContext.rejectReason = "content is abc";
            contentPublishingContext.tags = List.of("tag1", "tag2");
            contentPublishingContext.submitTime = new Timestamp(System.currentTimeMillis());
            contentPublishingContext.publishTime = new Timestamp(System.currentTimeMillis());
            contentPublishingContext.revisionCount = "1";
            return contentPublishingContext;
        }

        public static ContentPublishingContext NOMORE() {
            ContentPublishingContext contentPublishingContext = new ContentPublishingContext();
            contentPublishingContext.entityId = "12345";
            contentPublishingContext.auth = "admin";
            contentPublishingContext.content = "aaa";
            contentPublishingContext.authorId = "admin";
            contentPublishingContext.reviewerId = "123";
            contentPublishingContext.tags = List.of("tag1", "tag2");
            contentPublishingContext.submitTime = new Timestamp(System.currentTimeMillis());
            contentPublishingContext.publishTime = new Timestamp(System.currentTimeMillis());
            contentPublishingContext.revisionCount = "1";
            return contentPublishingContext;
        }
    }

    StateMachine<ContentPublishingStatusEnum, ContentPublishingEventEnum, ContentPublishingContext>
            stateMachine;

    @BeforeEach
    public void init() {
        StateMachineBuilder<
                        ContentPublishingStatusEnum,
                        ContentPublishingEventEnum,
                        ContentPublishingContext>
                builder = StateMachineBuilderFactory.create();
        // 草稿 提交 提交审核
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.DRAFT)
                .to(ContentPublishingStatusEnum.SUBMITTED)
                .on(ContentPublishingEventEnum.SUBMIT)
                .when(checkIsReady())
                .perform(doAction());
        // 提交审核 审核通过  审核通过进行格式转换
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.SUBMITTED)
                .to(ContentPublishingStatusEnum.PROCESSING)
                .on(ContentPublishingEventEnum.APPROVE)
                .when(checkCensorIsAgree())
                .perform(doAction());
        // 提交审核 审核拒绝 已经拒绝
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.SUBMITTED)
                .to(ContentPublishingStatusEnum.REJECTED)
                .on(ContentPublishingEventEnum.REJECT)
                .when(checkRejectCondition())
                .perform(doAction());
        // 已经拒绝 撤销拒绝 提交审核
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.REJECTED)
                .to(ContentPublishingStatusEnum.SUBMITTED)
                .on(ContentPublishingEventEnum.WITHDRAW)
                .when(checkWithdrawCondition())
                .perform(doAction());

        // 已经拒绝 修改重提 草稿状态
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.REJECTED)
                .to(ContentPublishingStatusEnum.DRAFT)
                .on(ContentPublishingEventEnum.REVISE)
                .when(checkReviseCondition())
                .perform(doAction());

        // 审核通过，进行格式转换和优化 发布 已经发布
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.PROCESSING)
                .to(ContentPublishingStatusEnum.PUBLISHED)
                .on(ContentPublishingEventEnum.PUBLISH)
                .when(checkPublishCondition())
                .perform(doAction());
        // 已经发布 取消发布  未发布
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.PUBLISHED)
                .to(ContentPublishingStatusEnum.UNPUBLISHED)
                .on(ContentPublishingEventEnum.UNPUBLISH)
                .when(checkUnpublishCondition())
                .perform(doAction());

        // 未发布 发布  已经发布
        builder.externalTransition()
                .from(ContentPublishingStatusEnum.UNPUBLISHED)
                .to(ContentPublishingStatusEnum.PUBLISHED)
                .on(ContentPublishingEventEnum.PUBLISH)
                .when(checkRepublishCondition())
                .perform(doAction());
        stateMachine = builder.build(MACHINE_ID);
    }

    @Test
    public void testExternalNormal() {

        ContentPublishingStatusEnum contentPublishingStatusEnum =
                stateMachine.fireEvent(
                        ContentPublishingStatusEnum.DRAFT,
                        ContentPublishingEventEnum.SUBMIT,
                        ContentPublishingContext.NOMORE());
        Assertions.assertEquals(ContentPublishingStatusEnum.SUBMITTED, contentPublishingStatusEnum);
    }

    @Test
    public void testUml() {
        stateMachine.showStateMachine();
        System.out.println(stateMachine.generatePlantUML());
    }

    private Condition<ContentPublishingContext> checkCondition() {
        return context -> {
            System.out.println("Check condition : " + context);
            return true;
        };
    }

    private Condition<ContentPublishingContext> checkCensorIsAgree() {
        return context -> {
            System.out.println("Check approval condition : " + context);
            // 审核通过的条件：
            // 1. 内容已准备好
            // 2. 有审核员ID
            // 3. 没有拒绝原因（表示审核通过）
            return context.isContentReady()
                    && context.reviewerId != null
                    && context.rejectReason == null;
        };
    }

    private Condition<ContentPublishingContext> checkRejectCondition() {
        return context -> {
            System.out.println("Check reject condition : " + context);
            // 审核拒绝的条件：
            // 1. 有审核员ID
            // 2. 有拒绝原因
            return context.reviewerId != null
                    && context.rejectReason != null
                    && !context.rejectReason.trim().isEmpty();
        };
    }

    private Condition<ContentPublishingContext> checkWithdrawCondition() {
        return context -> {
            System.out.println("Check withdraw condition : " + context);
            // 撤回条件：作者本人或管理员可以撤回被拒绝的内容
            return context.canWithdraw();
        };
    }

    private Condition<ContentPublishingContext> checkReviseCondition() {
        return context -> {
            System.out.println("Check revise condition : " + context);
            // 修改重提条件：作者本人可以修改被拒绝的内容
            return context.canRevise();
        };
    }

    private Condition<ContentPublishingContext> checkPublishCondition() {
        return context -> {
            System.out.println("Check publish condition : " + context);
            // 发布条件：内容已准备好且没有拒绝原因
            return context.canPublish();
        };
    }

    private Condition<ContentPublishingContext> checkUnpublishCondition() {
        return context -> {
            System.out.println("Check unpublish condition : " + context);
            // 取消发布条件：管理员或作者可以取消发布
            return context.canUnpublish();
        };
    }

    private Condition<ContentPublishingContext> checkRepublishCondition() {
        return context -> {
            System.out.println("Check republish condition : " + context);
            // 重新发布条件：内容已准备好且有发布权限
            return context.canPublish() && context.canUnpublish();
        };
    }

    private boolean doCensor(String content) {
        return "abc".equals(content);
    }

    private Condition<ContentPublishingContext> checkIsReady() {
        return context -> {
            System.out.println("Check is ready : " + context);
            return context.isContentReady();
        };
    }

    private Action<
                    ContentPublishingStatusEnum,
                    ContentPublishingEventEnum,
                    ContentPublishingContext>
            doAction() {
        return (from, to, event, ctx) -> {
            System.out.println("from: " + from + " to: " + to + " ctx: " + ctx);
        };
    }
}
