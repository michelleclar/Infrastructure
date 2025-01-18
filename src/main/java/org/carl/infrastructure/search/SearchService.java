package org.carl.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
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
import org.jboss.logging.Logger;

import java.awt.print.Book;
import java.io.IOException;
import java.io.StringReader;

// TODO: use json query to search ?
@ApplicationScoped
public class SearchService {
    @Inject ElasticsearchClient esClient;
    static final Logger log = Logger.getLogger(SearchService.class);

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

//package org.carl.infrastructure.search;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
//import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
//import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
//import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
//import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
//import co.elastic.clients.elasticsearch.core.SearchRequest;
//import jakarta.inject.Inject;
//import org.junit.jupiter.api.Test;
//
//import java.awt.print.Book;
//import java.io.IOException;
//import java.io.StringReader;
//
//import static org.junit.jupiter.api.Assertions.*;
// https://jenkinwang.github.io/2020/08/16/Elasticsearch%E8%87%AA%E5%AE%9A%E4%B9%89%E6%96%87%E6%A1%A3%E5%BE%97%E5%88%86%E5%B9%B6%E6%8E%92%E5%BA%8F/
//class SearchServiceTest {
//    @Inject
//    ElasticsearchClient client;
//    StringReader query = new StringReader("""
//            {
//              "query": {
//                "function_score": {
//                  "query": {
//                    "match_all": {}
//                  },
//                  "functions": [
//                    {
//                      "field_value_factor": {
//                        "field": "popularity",
//                        "factor": 1.5,
//                        "modifier": "log1p"
//                      }
//                    },
//                    {
//                      "filter": {
//                        "term": {
//                          "category": "sports"
//                        }
//                      },
//                      "weight": 2.0
//                    }
//                  ],
//                  "score_mode": "sum",
//                  "boost_mode": "sum"
//                }
//              },
//              "size": 10
//            }
//            """);
//
//    @Test
//    void createIndex() {
//    }
//
//    @Test
//    void index() {
//    }
//
//    @Test
//    void delete() {
//    }
//
//    @Test
//    void get() {
//    }
//
//    @Test
//    void search() throws IOException {
//        SearchRequest aggRequest = SearchRequest.of(b -> b
//                        .withJson(query)
////                .aggregations("max-cpu", a1 -> a1
////                        .dateHistogram(h -> h
////                                .field("@timestamp")
////                                .calendarInterval(CalendarInterval.Hour)
////                        )
////                        .aggregations("max", a2 -> a2
////                                .max(m -> m.field("host.cpu.usage"))
////                        )
////                )
//                        .size(5)
//        );
//        client.search(ss -> ss.index("archive").withJson(query), Book.class);
//        client.search(ss -> ss.index("archive").query(q -> q.functionScore(qe -> qe.functions(a -> a.fieldValueFactor(fvf -> fvf.field("name").factor(1.5).modifier(FieldValueFactorModifier.Log1p)).filter(f -> f.term(t -> t.field("a"))).weight(2.0))))
//                , Book.class);
//
//    }
//
//    @Test
//    void update() {
//    }
//
//    @Test
//    void testUpdate() {
//    }
//
//    //NOTE : https://github.com/elastic/elasticsearch-java/blob/main/examples/esql-article/src/main/java/co/elastic/clients/esql/article/EsqlArticle.java
//    @Test
//    void indices() throws IOException {
//        client.indices()
//                .create(c -> c
//                        .index("books")
//                        .mappings(mp -> mp
//                                .properties("title", p -> p.text(t -> t))
//                                .properties("description", p -> p.text(t -> t))
//                                .properties("author", p -> p.text(t -> t))
//                                .properties("year", p -> p.short_(s -> s))
//                                .properties("publisher", p -> p.text(t -> t))
//                                .properties("ratings", p -> p.halfFloat(hf -> hf))
//                        ));
//    }
//}
