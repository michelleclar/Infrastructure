package org.carl.infrastructure.pulsar.model;

import java.time.LocalDateTime;

public class TestNotification {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long userId;
    private Boolean isRead;
    private LocalDateTime createTime;
    private LocalDateTime readTime;

    public TestNotification() {}

    public TestNotification(
            Long id,
            String title,
            String content,
            String type,
            Long userId,
            Boolean isRead,
            LocalDateTime createTime,
            LocalDateTime readTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.userId = userId;
        this.isRead = isRead;
        this.createTime = createTime;
        this.readTime = readTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestNotification that = (TestNotification) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (isRead != null ? !isRead.equals(that.isRead) : that.isRead != null) return false;
        if (createTime != null ? !createTime.equals(that.createTime) : that.createTime != null)
            return false;
        return readTime != null ? readTime.equals(that.readTime) : that.readTime == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (isRead != null ? isRead.hashCode() : 0);
        result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
        result = 31 * result + (readTime != null ? readTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestNotification{"
                + "id="
                + id
                + ", title='"
                + title
                + '\''
                + ", content='"
                + content
                + '\''
                + ", type='"
                + type
                + '\''
                + ", userId="
                + userId
                + ", isRead="
                + isRead
                + ", createTime="
                + createTime
                + ", readTime="
                + readTime
                + '}';
    }
}
