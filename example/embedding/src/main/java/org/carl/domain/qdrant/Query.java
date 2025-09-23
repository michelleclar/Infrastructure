package org.carl.domain.qdrant;

public class Query {
    QueryType queryType;
    VectorQuery vector;
    RecommendQuery recommend;
    FormulaQuery formula;

    public QueryType getQueryType() {
        return queryType;
    }

    public Query setQueryType(QueryType queryType) {
        this.queryType = queryType;
        return this;
    }

    public VectorQuery getVector() {
        return vector;
    }

    public Query setVector(VectorQuery vector) {
        this.vector = vector;
        return this;
    }

    public RecommendQuery getRecommend() {
        return recommend;
    }

    public Query setRecommend(RecommendQuery recommend) {
        this.recommend = recommend;
        return this;
    }

    public FormulaQuery getFormula() {
        return formula;
    }

    public Query setFormula(FormulaQuery formula) {
        this.formula = formula;
        return this;
    }
}
