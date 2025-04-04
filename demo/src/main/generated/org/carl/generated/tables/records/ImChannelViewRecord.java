/*
 * This file is generated by jOOQ.
 */
package org.carl.generated.tables.records;

import org.carl.generated.tables.ImChannelView;
import org.carl.generated.tables.interfaces.IImChannelView;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class ImChannelViewRecord extends UpdatableRecordImpl<ImChannelViewRecord>
        implements IImChannelView {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.im_channel_view.channel_view_id</code>.
     */
    public ImChannelViewRecord setChannelViewId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>public.im_channel_view.channel_view_id</code>.
     */
    @Override
    public Long getChannelViewId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.im_channel_view.channel_id</code>. 频道id
     */
    public ImChannelViewRecord setChannelId(Long value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>public.im_channel_view.channel_id</code>. 频道id
     */
    @Override
    public Long getChannelId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>public.im_channel_view.channel_view</code>.
     */
    public ImChannelViewRecord setChannelView(JSONB value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>public.im_channel_view.channel_view</code>.
     */
    @Override
    public JSONB getChannelView() {
        return (JSONB) get(2);
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

    public void from(IImChannelView from) {
        setChannelViewId(from.getChannelViewId());
        setChannelId(from.getChannelId());
        setChannelView(from.getChannelView());
        resetChangedOnNotNull();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ImChannelViewRecord
     */
    public ImChannelViewRecord() {
        super(ImChannelView.IM_CHANNEL_VIEW);
    }

    /**
     * Create a detached, initialised ImChannelViewRecord
     */
    public ImChannelViewRecord(Long channelViewId, Long channelId, JSONB channelView) {
        super(ImChannelView.IM_CHANNEL_VIEW);

        setChannelViewId(channelViewId);
        setChannelId(channelId);
        setChannelView(channelView);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ImChannelViewRecord
     */
    public ImChannelViewRecord(org.carl.generated.tables.pojos.ImChannelView value) {
        super(ImChannelView.IM_CHANNEL_VIEW);

        if (value != null) {
            setChannelViewId(value.getChannelViewId());
            setChannelId(value.getChannelId());
            setChannelView(value.getChannelView());
            resetChangedOnNotNull();
        }
    }
}
