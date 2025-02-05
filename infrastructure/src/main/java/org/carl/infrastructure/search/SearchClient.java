package org.carl.infrastructure.search;

import java.util.Map;

public interface SearchClient {
    <T> T search(String query, Class<T> clazz);

    int update(String query, Map<String, Object> parameters);

    int delete(String query, Map<String, Object> parameters);

    int insert(String query, Map<String, Object> parameters);
}
