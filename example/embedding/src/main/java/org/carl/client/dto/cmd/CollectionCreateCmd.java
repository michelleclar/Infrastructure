package org.carl.client.dto.cmd;

import io.qdrant.client.grpc.Collections;

public class CollectionCreateCmd {
    String collectionName;
    Integer size;
    Collections.Distance distance;

    public Collections.Distance getDistance() {
        return distance;
    }

    public CollectionCreateCmd setDistance(Collections.Distance distance) {
        this.distance = distance;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public CollectionCreateCmd setSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public CollectionCreateCmd setCollectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }
}
