/*
 * This file is generated by jOOQ.
 */
package org.carl.generated.tables.daos;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.carl.generated.tables.ImChannelBox;
import org.carl.generated.tables.records.ImChannelBoxRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class ImChannelBoxDao
        extends DAOImpl<ImChannelBoxRecord, org.carl.generated.tables.pojos.ImChannelBox, Long> {

    /**
     * Create a new ImChannelBoxDao without any configuration
     */
    public ImChannelBoxDao() {
        super(ImChannelBox.IM_CHANNEL_BOX, org.carl.generated.tables.pojos.ImChannelBox.class);
    }

    /**
     * Create a new ImChannelBoxDao with an attached configuration
     */
    public ImChannelBoxDao(Configuration configuration) {
        super(
                ImChannelBox.IM_CHANNEL_BOX,
                org.carl.generated.tables.pojos.ImChannelBox.class,
                configuration);
    }

    @Override
    public Long getId(org.carl.generated.tables.pojos.ImChannelBox object) {
        return object.getChannelBoxId();
    }

    /**
     * Fetch records that have <code>channel_box_id BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfChannelBoxId(
            Long lowerInclusive, Long upperInclusive) {
        return fetchRange(
                ImChannelBox.IM_CHANNEL_BOX.CHANNEL_BOX_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>channel_box_id IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByChannelBoxId(Long... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.CHANNEL_BOX_ID, values);
    }

    /**
     * Fetch a unique record that has <code>channel_box_id = value</code>
     */
    public org.carl.generated.tables.pojos.ImChannelBox fetchOneByChannelBoxId(Long value) {
        return fetchOne(ImChannelBox.IM_CHANNEL_BOX.CHANNEL_BOX_ID, value);
    }

    /**
     * Fetch a unique record that has <code>channel_box_id = value</code>
     */
    public Optional<org.carl.generated.tables.pojos.ImChannelBox> fetchOptionalByChannelBoxId(
            Long value) {
        return fetchOptional(ImChannelBox.IM_CHANNEL_BOX.CHANNEL_BOX_ID, value);
    }

    /**
     * Fetch records that have <code>created_at BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfCreatedAt(
            OffsetDateTime lowerInclusive, OffsetDateTime upperInclusive) {
        return fetchRange(ImChannelBox.IM_CHANNEL_BOX.CREATED_AT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>created_at IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByCreatedAt(
            OffsetDateTime... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.CREATED_AT, values);
    }

    /**
     * Fetch records that have <code>updated_at BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfUpdatedAt(
            OffsetDateTime lowerInclusive, OffsetDateTime upperInclusive) {
        return fetchRange(ImChannelBox.IM_CHANNEL_BOX.UPDATED_AT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>updated_at IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByUpdatedAt(
            OffsetDateTime... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.UPDATED_AT, values);
    }

    /**
     * Fetch records that have <code>member_id BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfMemberId(
            Long lowerInclusive, Long upperInclusive) {
        return fetchRange(ImChannelBox.IM_CHANNEL_BOX.MEMBER_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>member_id IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByMemberId(Long... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.MEMBER_ID, values);
    }

    /**
     * Fetch records that have <code>is_top BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfIsTop(
            Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(ImChannelBox.IM_CHANNEL_BOX.IS_TOP, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>is_top IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByIsTop(Boolean... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.IS_TOP, values);
    }

    /**
     * Fetch records that have <code>is_delete BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfIsDelete(
            Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(ImChannelBox.IM_CHANNEL_BOX.IS_DELETE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>is_delete IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByIsDelete(Boolean... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.IS_DELETE, values);
    }

    /**
     * Fetch records that have <code>is_subscribe BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfIsSubscribe(
            Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(ImChannelBox.IM_CHANNEL_BOX.IS_SUBSCRIBE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>is_subscribe IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByIsSubscribe(
            Boolean... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.IS_SUBSCRIBE, values);
    }

    /**
     * Fetch records that have <code>channel_id BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchRangeOfChannelId(
            Long lowerInclusive, Long upperInclusive) {
        return fetchRange(ImChannelBox.IM_CHANNEL_BOX.CHANNEL_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>channel_id IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.ImChannelBox> fetchByChannelId(Long... values) {
        return fetch(ImChannelBox.IM_CHANNEL_BOX.CHANNEL_ID, values);
    }
}
