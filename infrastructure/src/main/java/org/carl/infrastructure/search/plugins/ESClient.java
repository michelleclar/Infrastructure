package org.carl.infrastructure.search.plugins;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.inject.Inject;
import java.util.Map;
import org.carl.infrastructure.search.SearchClient;

public class ESClient implements SearchClient {

    private ElasticsearchClient client;

    @Inject
    public void setClient(ElasticsearchClient client) {
        this.client = client;
    }

    public ElasticsearchClient getClient() {
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
