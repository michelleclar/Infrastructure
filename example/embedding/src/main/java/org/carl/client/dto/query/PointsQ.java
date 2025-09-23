package org.carl.client.dto.query;

import org.carl.component.dto.Query;
import org.carl.domain.qdrant.filter.Filter;

public class PointsQ extends Query {
    //    /**
    //     * require: optional
    //     *
    //     * <p>type: string or uint64 or list of strings or uint64s or any
    //     */
    //    Object shardKey;

    /**
     * require: optional
     *
     * <p>type: object or list of objects or any
     */
    PointsQ prefetch;

    /**
     * require: optional
     *
     * <p>type: list of doubles or object or list of lists of doubles or uint64 or string or object
     * or any
     */
    org.carl.domain.qdrant.Query query;

    /**
     * require: optional
     *
     * <p>type: string
     */
    String using;

    Filter filter;

    /**
     * require: optional
     *
     * <p>type: double
     */
    Double scoreThreshold;

    /**
     * require: required
     *
     * <p>type: int
     */
    int limit = 1;

    /**
     * require: required
     *
     * <p>type: int
     */
    int offset = 0;

    /**
     * require: required
     *
     * <p>type: boolean
     */
    boolean withVector = false;

    /**
     * require: required
     *
     * <p>type: boolean
     */
    boolean withPayload = false;

    public PointsQ getPrefetch() {
        return prefetch;
    }

    public PointsQ setPrefetch(PointsQ prefetch) {
        this.prefetch = prefetch;
        return this;
    }

    public org.carl.domain.qdrant.Query getQuery() {
        return query;
    }

    public PointsQ setQuery(org.carl.domain.qdrant.Query query) {
        this.query = query;
        return this;
    }

    public String getUsing() {
        return using;
    }

    public PointsQ setUsing(String using) {
        this.using = using;
        return this;
    }

    public Filter getFilter() {
        return filter;
    }

    public PointsQ setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public PointsQ setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public PointsQ setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public PointsQ setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public boolean isWithVector() {
        return withVector;
    }

    public PointsQ setWithVector(boolean withVector) {
        this.withVector = withVector;
        return this;
    }

    public boolean isWithPayload() {
        return withPayload;
    }

    public PointsQ setWithPayload(boolean withPayload) {
        this.withPayload = withPayload;
        return this;
    }
}
