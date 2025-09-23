package org.carl.domain.qdrant;

/**
 * <a
 * href="https://api.qdrant.tech/v-1-14-x/api-reference/search/query-points#request.body.query.Query%20Interface.Query.Formula%20Query.formula">qdrant
 * query api</a>
 */
public enum QueryType {
    vector,
    nearest,
    recommend,
    discover,
    context,
    order,
    fusion,
    formula,
    sample;
}
