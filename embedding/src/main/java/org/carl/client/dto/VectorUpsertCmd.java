package org.carl.client.dto;

import io.qdrant.client.grpc.Points;

import java.util.*;

/** qdrant grpc upsert */
public class VectorUpsertCmd {
    String collectionName;
    List<Points.PointStruct> points;

    public VectorUpsertCmd setPoints(List<Points.PointStruct> points) {
        this.points = points;
        return this;
    }

    public List<Points.PointStruct> getPoints() {
        return points;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public VectorUpsertCmd setCollectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }
}
