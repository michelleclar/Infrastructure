package org.carl.infra.search.ability;

import java.util.List;
import java.util.Map;

/**
 * Agnostic interface for search operations, completely shielding underlying ES classes.
 */
public interface ISearchOperations {
    boolean indexExists(String indexName);
    void createIndex(String indexName, String mappingJson);
    long count(String indexName);
    void indexDocument(String indexName, String id, Object document);
    int bulkIndex(String indexName, Map<String, Object> documents);
    <T> List<T> search(String indexName, String queryJson, Class<T> documentClass);
    String searchRaw(String indexName, String queryJson);
}
