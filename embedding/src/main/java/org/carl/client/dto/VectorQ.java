package org.carl.client.dto;

import io.qdrant.client.grpc.Points;
import jakarta.annotation.Nonnull;
import org.carl.component.dto.Query;
import org.carl.components.factory.PointIdFactory;

import java.util.List;
import java.util.UUID;

/** query qdrant use grpc client */
public class VectorQ extends Query {
    String collectionName;
    List<Float> vector;
    Integer limit;
    Boolean withPayload = true;
    Boolean withVector = false;
    Points.Filter filter;
    Points.PointId pointId;

    public Points.Filter getFilter() {
        return filter;
    }

    public VectorQ setFilter(Points.Filter filter) {
        this.filter = filter;
        return this;
    }

    public Boolean getWithVector() {
        return withVector;
    }

    public VectorQ setWithVector(Boolean withVector) {
        this.withVector = withVector;
        return this;
    }

    public Boolean getWithPayload() {
        return withPayload;
    }

    public VectorQ setWithPayload(Boolean withPayload) {
        this.withPayload = withPayload;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public VectorQ setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public List<Float> getVector() {
        return vector;
    }

    public VectorQ setVector(List<Float> vector) {
        this.vector = vector;
        return this;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public VectorQ setCollectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }

    public Points.PointId getPointId() {
        return pointId;
    }

    public VectorQ setPointId(Points.PointId pointId) {
        this.pointId = pointId;
        return this;
    }

    public VectorQ setPointId(long id) {
        this.pointId = PointIdFactory.id(id);
        return this;
    }

    public VectorQ setPointId(UUID uuid) {
        this.pointId = PointIdFactory.id(uuid);
        return this;
    }
}
