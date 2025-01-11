package org.carl.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.search.core.action.Delete;
import org.carl.infrastructure.search.core.action.Get;
import org.carl.infrastructure.search.core.action.Index;
import org.carl.infrastructure.search.core.action.Indices;
import org.carl.infrastructure.search.core.action.Search;
import org.carl.infrastructure.search.core.action.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: use json query to search ?
@ApplicationScoped
public class SearchService {
    @Inject ElasticsearchClient esClient;
    static final Logger log = LoggerFactory.getLogger(SearchService.class);

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
