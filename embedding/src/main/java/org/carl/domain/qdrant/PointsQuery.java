package org.carl.domain.qdrant;

import io.qdrant.client.grpc.Points;

public class PointsQuery {
    Integer limit;
    Integer offset;
    Boolean withPayload = true;
    Boolean withVector = false;
    Points.Query query;
    Points.Filter filter;
    Points.PointId pointId;
    Points.Formula filterFormula;
    Points.PrefetchQuery prefetchQuery;

    public Points.PrefetchQuery getPrefetchQuery() {
        return prefetchQuery;
    }

    public PointsQuery setPrefetchQuery(Points.PrefetchQuery prefetchQuery) {
        this.prefetchQuery = prefetchQuery;
        return this;
    }

    public Points.Formula getFilterFormula() {
        return filterFormula;
    }

    public PointsQuery setFilterFormula(Points.Formula filterFormula) {
        this.filterFormula = filterFormula;
        return this;
    }

    public Points.PointId getPointId() {
        return pointId;
    }

    public PointsQuery setPointId(Points.PointId pointId) {
        this.pointId = pointId;
        return this;
    }

    public Points.Filter getFilter() {
        return filter;
    }

    public PointsQuery setFilter(Points.Filter filter) {
        this.filter = filter;
        return this;
    }

    public Boolean getWithVector() {
        return withVector;
    }

    public PointsQuery setWithVector(Boolean withVector) {
        this.withVector = withVector;
        return this;
    }

    public Boolean getWithPayload() {
        return withPayload;
    }

    public PointsQuery setWithPayload(Boolean withPayload) {
        this.withPayload = withPayload;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public PointsQuery setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public PointsQuery setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public Points.Query getQuery() {
        return query;
    }

    public PointsQuery setQuery(Points.Query query) {
        this.query = query;
        return this;
    }
}
