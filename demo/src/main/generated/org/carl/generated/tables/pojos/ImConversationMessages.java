/*
 * This file is generated by jOOQ.
 */
package org.carl.generated.tables.pojos;

import java.time.OffsetDateTime;
import org.carl.generated.tables.interfaces.IImConversationMessages;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class ImConversationMessages implements IImConversationMessages {

    private static final long serialVersionUID = 1L;

    private Long conversationMessageId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long memberFromId;
    private Long memberToId;
    private Long messageId;
    private Boolean isRead;
    private Boolean isRollback;
    private Boolean isVisibleOwner;
    private Boolean isInteract;
    private String isAgree;
    private String scope;
    private Long conversationId;

    public ImConversationMessages() {}

    public ImConversationMessages(IImConversationMessages value) {
        this.conversationMessageId = value.getConversationMessageId();
        this.createdAt = value.getCreatedAt();
        this.updatedAt = value.getUpdatedAt();
        this.memberFromId = value.getMemberFromId();
        this.memberToId = value.getMemberToId();
        this.messageId = value.getMessageId();
        this.isRead = value.getIsRead();
        this.isRollback = value.getIsRollback();
        this.isVisibleOwner = value.getIsVisibleOwner();
        this.isInteract = value.getIsInteract();
        this.isAgree = value.getIsAgree();
        this.scope = value.getScope();
        this.conversationId = value.getConversationId();
    }

    public ImConversationMessages(
            Long conversationMessageId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long memberFromId,
            Long memberToId,
            Long messageId,
            Boolean isRead,
            Boolean isRollback,
            Boolean isVisibleOwner,
            Boolean isInteract,
            String isAgree,
            String scope,
            Long conversationId) {
        this.conversationMessageId = conversationMessageId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.memberFromId = memberFromId;
        this.memberToId = memberToId;
        this.messageId = messageId;
        this.isRead = isRead;
        this.isRollback = isRollback;
        this.isVisibleOwner = isVisibleOwner;
        this.isInteract = isInteract;
        this.isAgree = isAgree;
        this.scope = scope;
        this.conversationId = conversationId;
    }

    /**
     * Getter for
     * <code>public.im_conversation_messages.conversation_message_id</code>.
     */
    @Override
    public Long getConversationMessageId() {
        return this.conversationMessageId;
    }

    /**
     * Setter for
     * <code>public.im_conversation_messages.conversation_message_id</code>.
     */
    public ImConversationMessages setConversationMessageId(Long conversationMessageId) {
        this.conversationMessageId = conversationMessageId;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.created_at</code>.
     */
    @Override
    public OffsetDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Setter for <code>public.im_conversation_messages.created_at</code>.
     */
    public ImConversationMessages setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.updated_at</code>.
     */
    @Override
    public OffsetDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * Setter for <code>public.im_conversation_messages.updated_at</code>.
     */
    public ImConversationMessages setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.member_from_id</code>.
     * 会话消息发送者
     */
    @Override
    public Long getMemberFromId() {
        return this.memberFromId;
    }

    /**
     * Setter for <code>public.im_conversation_messages.member_from_id</code>.
     * 会话消息发送者
     */
    public ImConversationMessages setMemberFromId(Long memberFromId) {
        this.memberFromId = memberFromId;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.member_to_id</code>.
     * 会话消息接受者
     */
    @Override
    public Long getMemberToId() {
        return this.memberToId;
    }

    /**
     * Setter for <code>public.im_conversation_messages.member_to_id</code>.
     * 会话消息接受者
     */
    public ImConversationMessages setMemberToId(Long memberToId) {
        this.memberToId = memberToId;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.message_id</code>.
     */
    @Override
    public Long getMessageId() {
        return this.messageId;
    }

    /**
     * Setter for <code>public.im_conversation_messages.message_id</code>.
     */
    public ImConversationMessages setMessageId(Long messageId) {
        this.messageId = messageId;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.is_read</code>. 是否已读
     */
    @Override
    public Boolean getIsRead() {
        return this.isRead;
    }

    /**
     * Setter for <code>public.im_conversation_messages.is_read</code>. 是否已读
     */
    public ImConversationMessages setIsRead(Boolean isRead) {
        this.isRead = isRead;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.is_rollback</code>. 是否回滚
     */
    @Override
    public Boolean getIsRollback() {
        return this.isRollback;
    }

    /**
     * Setter for <code>public.im_conversation_messages.is_rollback</code>. 是否回滚
     */
    public ImConversationMessages setIsRollback(Boolean isRollback) {
        this.isRollback = isRollback;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.is_visible_owner</code>.
     * 是否对自己可见
     */
    @Override
    public Boolean getIsVisibleOwner() {
        return this.isVisibleOwner;
    }

    /**
     * Setter for <code>public.im_conversation_messages.is_visible_owner</code>.
     * 是否对自己可见
     */
    public ImConversationMessages setIsVisibleOwner(Boolean isVisibleOwner) {
        this.isVisibleOwner = isVisibleOwner;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.is_interact</code>.
     * 是否是互动消息
     */
    @Override
    public Boolean getIsInteract() {
        return this.isInteract;
    }

    /**
     * Setter for <code>public.im_conversation_messages.is_interact</code>.
     * 是否是互动消息
     */
    public ImConversationMessages setIsInteract(Boolean isInteract) {
        this.isInteract = isInteract;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.is_agree</code>.
     * 当前互动消息状态
     */
    @Override
    public String getIsAgree() {
        return this.isAgree;
    }

    /**
     * Setter for <code>public.im_conversation_messages.is_agree</code>.
     * 当前互动消息状态
     */
    public ImConversationMessages setIsAgree(String isAgree) {
        this.isAgree = isAgree;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.scope</code>. 获取域
     */
    @Override
    public String getScope() {
        return this.scope;
    }

    /**
     * Setter for <code>public.im_conversation_messages.scope</code>. 获取域
     */
    public ImConversationMessages setScope(String scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Getter for <code>public.im_conversation_messages.conversation_id</code>.
     */
    @Override
    public Long getConversationId() {
        return this.conversationId;
    }

    /**
     * Setter for <code>public.im_conversation_messages.conversation_id</code>.
     */
    public ImConversationMessages setConversationId(Long conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final ImConversationMessages other = (ImConversationMessages) obj;
        if (this.conversationMessageId == null) {
            if (other.conversationMessageId != null) return false;
        } else if (!this.conversationMessageId.equals(other.conversationMessageId)) return false;
        if (this.createdAt == null) {
            if (other.createdAt != null) return false;
        } else if (!this.createdAt.equals(other.createdAt)) return false;
        if (this.updatedAt == null) {
            if (other.updatedAt != null) return false;
        } else if (!this.updatedAt.equals(other.updatedAt)) return false;
        if (this.memberFromId == null) {
            if (other.memberFromId != null) return false;
        } else if (!this.memberFromId.equals(other.memberFromId)) return false;
        if (this.memberToId == null) {
            if (other.memberToId != null) return false;
        } else if (!this.memberToId.equals(other.memberToId)) return false;
        if (this.messageId == null) {
            if (other.messageId != null) return false;
        } else if (!this.messageId.equals(other.messageId)) return false;
        if (this.isRead == null) {
            if (other.isRead != null) return false;
        } else if (!this.isRead.equals(other.isRead)) return false;
        if (this.isRollback == null) {
            if (other.isRollback != null) return false;
        } else if (!this.isRollback.equals(other.isRollback)) return false;
        if (this.isVisibleOwner == null) {
            if (other.isVisibleOwner != null) return false;
        } else if (!this.isVisibleOwner.equals(other.isVisibleOwner)) return false;
        if (this.isInteract == null) {
            if (other.isInteract != null) return false;
        } else if (!this.isInteract.equals(other.isInteract)) return false;
        if (this.isAgree == null) {
            if (other.isAgree != null) return false;
        } else if (!this.isAgree.equals(other.isAgree)) return false;
        if (this.scope == null) {
            if (other.scope != null) return false;
        } else if (!this.scope.equals(other.scope)) return false;
        if (this.conversationId == null) {
            if (other.conversationId != null) return false;
        } else if (!this.conversationId.equals(other.conversationId)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result =
                prime * result
                        + ((this.conversationMessageId == null)
                                ? 0
                                : this.conversationMessageId.hashCode());
        result = prime * result + ((this.createdAt == null) ? 0 : this.createdAt.hashCode());
        result = prime * result + ((this.updatedAt == null) ? 0 : this.updatedAt.hashCode());
        result = prime * result + ((this.memberFromId == null) ? 0 : this.memberFromId.hashCode());
        result = prime * result + ((this.memberToId == null) ? 0 : this.memberToId.hashCode());
        result = prime * result + ((this.messageId == null) ? 0 : this.messageId.hashCode());
        result = prime * result + ((this.isRead == null) ? 0 : this.isRead.hashCode());
        result = prime * result + ((this.isRollback == null) ? 0 : this.isRollback.hashCode());
        result =
                prime * result
                        + ((this.isVisibleOwner == null) ? 0 : this.isVisibleOwner.hashCode());
        result = prime * result + ((this.isInteract == null) ? 0 : this.isInteract.hashCode());
        result = prime * result + ((this.isAgree == null) ? 0 : this.isAgree.hashCode());
        result = prime * result + ((this.scope == null) ? 0 : this.scope.hashCode());
        result =
                prime * result
                        + ((this.conversationId == null) ? 0 : this.conversationId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ImConversationMessages (");

        sb.append(conversationMessageId);
        sb.append(", ").append(createdAt);
        sb.append(", ").append(updatedAt);
        sb.append(", ").append(memberFromId);
        sb.append(", ").append(memberToId);
        sb.append(", ").append(messageId);
        sb.append(", ").append(isRead);
        sb.append(", ").append(isRollback);
        sb.append(", ").append(isVisibleOwner);
        sb.append(", ").append(isInteract);
        sb.append(", ").append(isAgree);
        sb.append(", ").append(scope);
        sb.append(", ").append(conversationId);

        sb.append(")");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    public void from(IImConversationMessages from) {
        setConversationMessageId(from.getConversationMessageId());
        setCreatedAt(from.getCreatedAt());
        setUpdatedAt(from.getUpdatedAt());
        setMemberFromId(from.getMemberFromId());
        setMemberToId(from.getMemberToId());
        setMessageId(from.getMessageId());
        setIsRead(from.getIsRead());
        setIsRollback(from.getIsRollback());
        setIsVisibleOwner(from.getIsVisibleOwner());
        setIsInteract(from.getIsInteract());
        setIsAgree(from.getIsAgree());
        setScope(from.getScope());
        setConversationId(from.getConversationId());
    }
}
