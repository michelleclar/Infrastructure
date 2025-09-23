package org.carl.domain.qdrant.filter;

import org.carl.domain.qdrant.filter.condition.*;

import java.util.List;

/**
 * "filter": { "must": [ { "key": "city", "match": { "value": "London" } } ] },
 *
 * <p>`must should mustNot minShould` can be together {@link Condition}
 */
public class Filter {
    private List<Condition> must;
    private List<Condition> should;
    private List<Condition> mustNot;

    public List<Condition> getMustNot() {
        return mustNot;
    }

    public Filter setMustNot(List<Condition> mustNot) {
        this.mustNot = mustNot;
        return this;
    }

    public List<Condition> getShould() {
        return should;
    }

    public Filter setShould(List<Condition> should) {
        this.should = should;
        return this;
    }

    public List<Condition> getMust() {
        return must;
    }

    public Filter setMust(List<Condition> must) {
        this.must = must;
        return this;
    }
}
