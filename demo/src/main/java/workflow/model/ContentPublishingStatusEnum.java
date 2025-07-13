package workflow.model;

public enum ContentPublishingStatusEnum {

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
