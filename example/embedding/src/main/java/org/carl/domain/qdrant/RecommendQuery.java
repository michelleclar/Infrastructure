package org.carl.domain.qdrant;

import io.qdrant.client.grpc.Points;

import java.util.List;

public class RecommendQuery {

    /** long uuid */
    List<String> positiveIds;

    /** need vectors text */
    List<String> positiveVectorTexts;

    /** recommend positive vectors */
    List<List<Float>> positiveVectors;

    /** negative long uuid */
    List<String> negativeIds;

    /** negative vectors text */
    List<String> negativeVectorTexts;

    /** negative vectors */
    List<List<Float>> negativeVectors;

    /** strategy */
    Points.RecommendStrategy strategy;

    public RecommendQuery setStrategy(Points.RecommendStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public RecommendQuery setNegativeVectors(List<List<Float>> negativeVectors) {
        this.negativeVectors = negativeVectors;
        return this;
    }

    public RecommendQuery setNegativeVectorTexts(List<String> negativeVectorTexts) {
        this.negativeVectorTexts = negativeVectorTexts;
        return this;
    }

    public RecommendQuery setNegativeIds(List<String> negativeIds) {
        this.negativeIds = negativeIds;
        return this;
    }

    public RecommendQuery setPositiveVectors(List<List<Float>> positiveVectors) {
        this.positiveVectors = positiveVectors;
        return this;
    }

    public RecommendQuery setPositiveVectorTexts(List<String> positiveVectorTexts) {
        this.positiveVectorTexts = positiveVectorTexts;
        return this;
    }

    public RecommendQuery setPositiveIds(List<String> positiveIds) {
        this.positiveIds = positiveIds;
        return this;
    }
}
