package org.carl.infrastructure.search.plugins.es.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

public class ESContext {
    ElasticsearchClient client;

    public ESContext(ElasticsearchClient client) {
        this.client = client;
    }
}
