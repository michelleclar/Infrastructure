package org.carl.infrastructure.search;

import java.util.Map;

public interface ElasticsearchClient extends SearchClient {
    void indexMapping(String index, String type, String mapping);

    @Override
    default int insert(String query, Map<String, Object> parameters) {
        return 0;
    }

    @Override
    default int update(String query, Map<String, Object> parameters) {
        return 0;
    }

    @Override
    default int delete(String query, Map<String, Object> parameters) {
        return 0;
    }

    default <T> T search(String query, Map<String, Object> parameters) {
        return null;
    }

    @Override
    default <T> T search(String query, Class<T> clazz) {
        return null;
    }
    ;

    default boolean reBuildIndex() {
        return false;
    }
    ;
}
