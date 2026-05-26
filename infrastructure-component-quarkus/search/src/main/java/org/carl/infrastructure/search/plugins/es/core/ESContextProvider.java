package org.carl.infrastructure.search.plugins.es.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class ESContextProvider {
    @Inject ElasticsearchClient client;

    @Produces
    @ApplicationScoped
    public ESContext get() {
        return new ESContext(client);
    }
}
