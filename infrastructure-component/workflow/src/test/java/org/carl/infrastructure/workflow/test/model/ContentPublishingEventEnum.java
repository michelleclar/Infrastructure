package org.carl.infrastructure.workflow.test.model;

public enum ContentPublishingEventEnum {
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
