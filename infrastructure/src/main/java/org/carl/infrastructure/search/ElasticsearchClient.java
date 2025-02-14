package org.carl.infrastructure.search;

import jakarta.inject.Inject;
import java.util.Map;

public class ElasticsearchClient implements SearchClient {

    private co.elastic.clients.elasticsearch.ElasticsearchClient client;

    @Inject
    public void setClient(co.elastic.clients.elasticsearch.ElasticsearchClient client) {
        this.client = client;
    }

    public co.elastic.clients.elasticsearch.ElasticsearchClient getClient() {
        return client;
    }

    @Override
    public int insert(String query, Map<String, Object> parameters) {
        return 0;
    }

    @Override
    public int update(String query, Map<String, Object> parameters) {
        return 0;
    }

    @Override
    public int delete(String query, Map<String, Object> parameters) {
        return 0;
    }

    public <T> T search(String query, Map<String, Object> parameters) {
        return null;
    }

    @Override
    public <T> T search(String query, Class<T> clazz) {
        return null;
    }
}
