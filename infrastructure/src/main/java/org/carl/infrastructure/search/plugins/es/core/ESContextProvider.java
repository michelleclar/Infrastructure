package org.carl.infrastructure.search.plugins.es.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class ESContextProvider implements Provider<ESContext> {
    @Inject ElasticsearchClient client;

    @Produces
    @ApplicationScoped
    @Override
    public ESContext get() {
        return new ESContext(client);
    }
}
