package org.carl.infrastructure.search.plugins.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;

import jakarta.inject.Inject;

import org.carl.infrastructure.search.plugins.es.core.action.Delete;
import org.carl.infrastructure.search.plugins.es.core.action.Get;
import org.carl.infrastructure.search.plugins.es.core.action.Index;
import org.carl.infrastructure.search.plugins.es.core.action.Indices;
import org.carl.infrastructure.search.plugins.es.core.action.Search;
import org.carl.infrastructure.search.plugins.es.core.action.Update;
import org.jboss.logging.Logger;

// TODO: use json query to search ?
public class ESSearchService {
    @Inject ElasticsearchClient esClient;
    static final Logger log = Logger.getLogger(ESSearchService.class);

    public CreateIndexResponse createIndex(String indexName) {
        return this.indices().create(indexName);
    }

    public <T> Index<T> index() {
        IndexRequest.Builder<T> builder = new IndexRequest.Builder<>();
        return new Index<>(esClient, builder);
    }

    public Delete delete() {
        DeleteRequest.Builder builder = new DeleteRequest.Builder();
        return new Delete(esClient, builder);
    }

    public Get get() {
        GetRequest.Builder builder = new GetRequest.Builder();
        return new Get(esClient, builder);
    }

    public Search search() {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        return new Search(esClient, builder);
    }

    // NOTE: T need update doc, K need read es doc
    public <T> Update<T, T> update(T document) {
        UpdateRequest.Builder<T, T> builder = new UpdateRequest.Builder<>();
        return new Update<>(esClient, builder, document);
    }

    public <T, K> Update<T, K> update(T document, K tPartialDocument) {
        UpdateRequest.Builder<T, K> builder = new UpdateRequest.Builder<>();
        return new Update<>(esClient, builder, document, tPartialDocument);
    }

    public Indices indices() {
        ElasticsearchIndicesClient indices = esClient.indices();
        return new Indices(indices);
    }
}
