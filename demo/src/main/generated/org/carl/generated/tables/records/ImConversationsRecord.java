/*
 * This file is generated by jOOQ.
 */
package org.carl.generated.tables.records;

import java.time.OffsetDateTime;
import org.carl.generated.tables.ImConversations;
import org.carl.generated.tables.interfaces.IImConversations;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class ImConversationsRecord extends UpdatableRecordImpl<ImConversationsRecord>
        implements IImConversations {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.im_conversations.conversation_id</code>.
     */
    public ImConversationsRecord setConversationId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>public.im_conversations.conversation_id</code>.
     */
    @Override
    public Long getConversationId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.im_conversations.created_at</code>.
     */
    public ImConversationsRecord setCreatedAt(OffsetDateTime value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>public.im_conversations.created_at</code>.
     */
    @Override
    public OffsetDateTime getCreatedAt() {
        return (OffsetDateTime) get(1);
    }

    /**
     * Setter for <code>public.im_conversations.updated_at</code>.
     */
    public ImConversationsRecord setUpdatedAt(OffsetDateTime value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>public.im_conversations.updated_at</code>.
     */
    @Override
    public OffsetDateTime getUpdatedAt() {
        return (OffsetDateTime) get(2);
    }

    /**
     * Setter for <code>public.im_conversations.member_from_id</code>. 会话发起者
     */
    public ImConversationsRecord setMemberFromId(Long value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>public.im_conversations.member_from_id</code>. 会话发起者
     */
    @Override
    public Long getMemberFromId() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>public.im_conversations.member_to_id</code>.
     */
    public ImConversationsRecord setMemberToId(Long value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>public.im_conversations.member_to_id</code>.
     */
    @Override
    public Long getMemberToId() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>public.im_conversations.type</code>. 会话类型
     */
    public ImConversationsRecord setType(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>public.im_conversations.type</code>. 会话类型
     */
    @Override
    public String getType() {
        return (String) get(5);
    }

    /**
     * Setter for <code>public.im_conversations.channel_id</code>.
     */
    public ImConversationsRecord setChannelId(Long value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>public.im_conversations.channel_id</code>.
     */
    @Override
    public Long getChannelId() {
        return (Long) get(6);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    public void from(IImConversations from) {
        setConversationId(from.getConversationId());
        setCreatedAt(from.getCreatedAt());
        setUpdatedAt(from.getUpdatedAt());
        setMemberFromId(from.getMemberFromId());
        setMemberToId(from.getMemberToId());
        setType(from.getType());
        setChannelId(from.getChannelId());
        resetChangedOnNotNull();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ImConversationsRecord
     */
    public ImConversationsRecord() {
        super(ImConversations.IM_CONVERSATIONS);
    }

    /**
     * Create a detached, initialised ImConversationsRecord
     */
    public ImConversationsRecord(
            Long conversationId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long memberFromId,
            Long memberToId,
            String type,
            Long channelId) {
        super(ImConversations.IM_CONVERSATIONS);

        setConversationId(conversationId);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
        setMemberFromId(memberFromId);
        setMemberToId(memberToId);
        setType(type);
        setChannelId(channelId);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ImConversationsRecord
     */
    public ImConversationsRecord(org.carl.generated.tables.pojos.ImConversations value) {
        super(ImConversations.IM_CONVERSATIONS);

        if (value != null) {
            setConversationId(value.getConversationId());
            setCreatedAt(value.getCreatedAt());
            setUpdatedAt(value.getUpdatedAt());
            setMemberFromId(value.getMemberFromId());
            setMemberToId(value.getMemberToId());
            setType(value.getType());
            setChannelId(value.getChannelId());
            resetChangedOnNotNull();
        }
    }
}
