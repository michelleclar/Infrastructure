package org.carl.domain.qdrant.filter.condition;

import org.carl.domain.qdrant.filter.Filter;

import java.util.List;

public class Condition {
    private Match match;
    private MatchKeyword matchKeyword;
    private MatchKeywords matchKeywords;
    private DataTimeRange datetimeRange;
    private String hisId;
    private List<String> hisIds;
    private String isNull;
    private String isEmpty;
    private NestedCondition nestedCondition;
    private NestedFilter nestedFilter;
    private Filter filter;

    public Filter getFilter() {
        return filter;
    }

    public Condition setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public NestedFilter getNestedFilter() {
        return nestedFilter;
    }

    public Condition setNestedFilter(NestedFilter nestedFilter) {
        this.nestedFilter = nestedFilter;
        return this;
    }

    public NestedCondition getNestedCondition() {
        return nestedCondition;
    }

    public Condition setNestedCondition(NestedCondition nestedCondition) {
        this.nestedCondition = nestedCondition;
        return this;
    }

    public String getIsEmpty() {
        return isEmpty;
    }

    public Condition setIsEmpty(String isEmpty) {
        this.isEmpty = isEmpty;
        return this;
    }

    public String getIsNull() {
        return isNull;
    }

    public Condition setIsNull(String isNull) {
        this.isNull = isNull;
        return this;
    }

    public List<String> getHisIds() {
        return hisIds;
    }

    public Condition setHisIds(List<String> hisIds) {
        this.hisIds = hisIds;
        return this;
    }

    public String getHisId() {
        return hisId;
    }

    public Condition setHisId(String hisId) {
        this.hisId = hisId;
        return this;
    }

    public DataTimeRange getDatetimeRange() {
        return datetimeRange;
    }

    public Condition setDatetimeRange(DataTimeRange datetimeRange) {
        this.datetimeRange = datetimeRange;
        return this;
    }

    public MatchKeywords getMatchKeywords() {
        return matchKeywords;
    }

    public Condition setMatchKeywords(MatchKeywords matchKeywords) {
        this.matchKeywords = matchKeywords;
        return this;
    }

    public MatchKeyword getMatchKeyword() {
        return matchKeyword;
    }

    public Condition setMatchKeyword(MatchKeyword matchKeyword) {
        this.matchKeyword = matchKeyword;
        return this;
    }

    public Match getMatch() {
        return match;
    }

    public Condition setMatch(Match match) {
        this.match = match;
        return this;
    }

    @Override
    public String toString() {
        return "{"
                + "        \"match\":"
                + match
                + ",         \"matchKeyword\":"
                + matchKeyword
                + ",         \"matchKeywords\":"
                + matchKeywords
                + ",         \"datetimeRange\":"
                + datetimeRange
                + ",         \"hisId\":\""
                + hisId
                + "\""
                + ",         \"hisIds\":"
                + hisIds
                + ",         \"isNull\":\""
                + isNull
                + "\""
                + ",         \"isEmpty\":\""
                + isEmpty
                + "\""
                + ",         \"nestedCondition\":"
                + nestedCondition
                + ",         \"nestedFilter\":"
                + nestedFilter
                + ",         \"filter\":"
                + filter
                + "}";
    }
}
