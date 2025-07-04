package org.carl.client.dto.query;

import io.qdrant.client.grpc.Points;

import org.carl.domain.qdrant.filter.Filter;

import java.util.List;

import javax.management.Query;

/**
 * NOTE: recommend api
 *
 * <p>is outer from web
 */
public class RecommendationQ extends Query {
    String vectorCollection;
    int limit = 1;

    /** string or uint64 or list of strings or uint64s or any */
    Object shardKey;

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

    Filter filter;

    Object params;
    Integer offset;

    boolean withPayload = false;

    boolean withVector = false;

    Double scoreThreshold;
    String using;

    String lookupFrom;

    public String getLookupFrom() {
        return lookupFrom;
    }

    public RecommendationQ setLookupFrom(String lookupFrom) {
        this.lookupFrom = lookupFrom;
        return this;
    }

    public String getUsing() {
        return using;
    }

    public RecommendationQ setUsing(String using) {
        this.using = using;
        return this;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public RecommendationQ setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
        return this;
    }

    public boolean isWithVector() {
        return withVector;
    }

    public RecommendationQ setWithVector(boolean withVector) {
        this.withVector = withVector;
        return this;
    }

    public boolean isWithPayload() {
        return withPayload;
    }

    public RecommendationQ setWithPayload(boolean withPayload) {
        this.withPayload = withPayload;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public RecommendationQ setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public Object getParams() {
        return params;
    }

    public RecommendationQ setParams(Object params) {
        this.params = params;
        return this;
    }

    public Filter getFilter() {
        return filter;
    }

    public RecommendationQ setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public Points.RecommendStrategy getStrategy() {
        return strategy;
    }

    public RecommendationQ setStrategy(Points.RecommendStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public List<String> getNegativeVectorTexts() {
        return negativeVectorTexts;
    }

    /**
     * need vectors text
     *
     * @param negativeVectorTexts vectors
     * @return this
     */
    public RecommendationQ setNegativeVectorTexts(List<String> negativeVectorTexts) {
        this.negativeVectorTexts = negativeVectorTexts;
        return this;
    }

    public List<String> getNegativeIds() {
        return negativeIds;
    }

    /**
     * @param negativeIds UUID long
     * @return this
     */
    public RecommendationQ setNegativeIds(List<String> negativeIds) {
        this.negativeIds = negativeIds;
        return this;
    }

    public List<String> getPositiveVectorTexts() {
        return positiveVectorTexts;
    }

    /**
     * need vectors text
     *
     * @param positiveVectorTexts vectors
     * @return this
     */
    public RecommendationQ setPositiveVectorTexts(List<String> positiveVectorTexts) {
        this.positiveVectorTexts = positiveVectorTexts;
        return this;
    }

    public List<String> getPositiveIds() {
        return positiveIds;
    }

    /**
     * @param positiveIds UUID long
     * @return this
     */
    public RecommendationQ setPositiveIds(List<String> positiveIds) {
        this.positiveIds = positiveIds;
        return this;
    }

    public Object getShardKey() {
        return shardKey;
    }

    /**
     * Specify in which shards to look for the points, if not specified - look in all shards
     *
     * @param shardKey string or uint64 or list of strings or uint64s or any
     * @return this
     */
    public RecommendationQ setShardKey(Object shardKey) {
        this.shardKey = shardKey;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public RecommendationQ setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public String getVectorCollection() {
        return vectorCollection;
    }

    public RecommendationQ setVectorCollection(String vectorCollection) {
        this.vectorCollection = vectorCollection;
        return this;
    }

    public List<List<Float>> getNegativeVectors() {
        return negativeVectors;
    }

    public RecommendationQ setNegativeVectors(List<List<Float>> negativeVectors) {
        this.negativeVectors = negativeVectors;
        return this;
    }

    public List<List<Float>> getPositiveVectors() {
        return positiveVectors;
    }

    public RecommendationQ setPositiveVectors(List<List<Float>> positiveVectors) {
        this.positiveVectors = positiveVectors;
        return this;
    }
}
