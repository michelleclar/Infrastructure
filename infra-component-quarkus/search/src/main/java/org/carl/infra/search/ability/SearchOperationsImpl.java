package org.carl.infra.search.ability;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SearchOperationsImpl implements ISearchOperations {

    @Inject
    ElasticsearchClient client;

    @Inject
    RestClient restClient;

    @Override
    public boolean indexExists(String indexName) {
        try {
            return client.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check index existence", e);
        }
    }

    @Override
    public void createIndex(String indexName, String mappingJson) {
        try {
            client.indices().create(CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .withJson(new StringReader(mappingJson))
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create index", e);
        }
    }

    @Override
    public long count(String indexName) {
        try {
            return client.count(c -> c.index(indexName)).count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count", e);
        }
    }

    @Override
    public void indexDocument(String indexName, String id, Object document) {
        try {
            client.index(IndexRequest.of(i -> i
                    .index(indexName)
                    .id(id)
                    .document(document)
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to index document", e);
        }
    }

    @Override
    public int bulkIndex(String indexName, Map<String, Object> documents) {
        try {
            var response = client.bulk(BulkRequest.of(b -> {
                for (var entry : documents.entrySet()) {
                    b.operations(op -> op
                            .index(idx -> idx
                                    .index(indexName)
                                    .id(entry.getKey())
                                    .document(entry.getValue())
                            )
                    );
                }
                return b;
            }));
            if (response.errors()) {
                throw new RuntimeException("Bulk indexing had errors");
            }
            return response.items().size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to bulk index", e);
        }
    }

    @Override
    public <T> List<T> search(String indexName, String queryJson, Class<T> documentClass) {
        try {
            SearchResponse<T> response = client.search(SearchRequest.of(s -> s
                    .index(indexName)
                    .withJson(new StringReader(queryJson))
            ), documentClass);
            return response.hits().hits().stream().map(h -> h.source()).toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to search", e);
        }
    }

    @Override
    public String searchRaw(String indexName, String queryJson) {
        try {
            Request request = new Request("POST", "/" + indexName + "/_search");
            request.setJsonEntity(queryJson);
            Response response = restClient.performRequest(request);
            try (InputStream is = response.getEntity().getContent()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search raw", e);
        }
    }
}
