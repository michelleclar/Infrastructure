package org.carl.infrastructure.workflow.test.model;

import java.sql.Timestamp;
import java.util.List;

public class ContentPublishingContext {
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

    public String getEntityId() {
        return entityId;
    }

    public ContentPublishingContext setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getAuth() {
        return auth;
    }

    public ContentPublishingContext setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public String getContent() {
        return content;
    }

    public ContentPublishingContext setContent(String content) {
        this.content = content;
        return this;
    }

    public String getAuthorId() {
        return authorId;
    }

    public ContentPublishingContext setAuthorId(String authorId) {
        this.authorId = authorId;
        return this;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public ContentPublishingContext setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
        return this;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public ContentPublishingContext setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public ContentPublishingContext setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public Timestamp getSubmitTime() {
        return submitTime;
    }

    public ContentPublishingContext setSubmitTime(Timestamp submitTime) {
        this.submitTime = submitTime;
        return this;
    }

    public Timestamp getPublishTime() {
        return publishTime;
    }

    public ContentPublishingContext setPublishTime(Timestamp publishTime) {
        this.publishTime = publishTime;
        return this;
    }

    public String getRevisionCount() {
        return revisionCount;
    }

    public ContentPublishingContext setRevisionCount(String revisionCount) {
        this.revisionCount = revisionCount;
        return this;
    }
}
