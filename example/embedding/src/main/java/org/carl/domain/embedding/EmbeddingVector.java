package org.carl.domain.embedding;

import java.util.List;

public class EmbeddingVector {
    private List<Float> vector;
    private long size;

    public List<Float> getVector() {
        return vector;
    }

    public EmbeddingVector setVector(List<Float> vector) {
        this.vector = vector;
        return this;
    }

    public long getSize() {
        return size;
    }

    public EmbeddingVector setSize(long size) {
        this.size = size;
        return this;
    }
}
