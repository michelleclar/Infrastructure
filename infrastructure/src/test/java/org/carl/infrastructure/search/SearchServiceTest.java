package org.carl.infrastructure.search;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class SearchServiceTest {
    //    @Inject ElasticsearchClient client;
    StringReader query =
            new StringReader(
                    """
                    {
                      "query": {
                        "function_score": {
                          "query": {
                            "match_all": {}
                          },
                          "functions": [
                            {
                              "field_value_factor": {
                                "field": "popularity",
                                "factor": 1.5,
                                "modifier": "log1p"
                              }
                            },
                            {
                              "filter": {
                                "term": {
                                  "category": "sports"
                                }
                              },
                              "weight": 2.0
                            }
                          ],
                          "score_mode": "sum",
                          "boost_mode": "sum"
                        }
                      },
                      "size": 10
                    }
                    """);

    @Test
    void createIndex() {}

    @Test
    void index() {}

    @Test
    void delete() {}

    @Test
    void get() {}

    //    @Test
    //    void search() throws IOException {
    //        SearchRequest aggRequest =
    //                SearchRequest.of(
    //                        b ->
    //                                b.withJson(query)
    //                                        //                .aggregations("max-cpu", a1 -> a1
    //                                        //                        .dateHistogram(h -> h
    //                                        //                                .field("@timestamp")
    //                                        //
    //                                        // .calendarInterval(CalendarInterval.Hour)
    //                                        //                        )
    //                                        //                        .aggregations("max", a2 ->
    // a2
    //                                        //                                .max(m ->
    //                                        // m.field("host.cpu.usage"))
    //                                        //                        )
    //                                        //                )
    //                                        .size(5));
    //        client.search(ss -> ss.index("archive").withJson(query), Book.class);
    //        client.search(
    //                ss ->
    //                        ss.index("archive")
    //                                .query(
    //                                        q ->
    //                                                q.functionScore(
    //                                                        qe ->
    //                                                                qe.functions(
    //                                                                        a ->
    //
    // a.fieldValueFactor(
    //
    //  fvf ->
    //
    //          fvf.field(
    //
    //                          "name")
    //
    //                  .factor(
    //
    //                          1.5)
    //
    //                  .modifier(
    //
    //                          FieldValueFactorModifier
    //
    //                                  .Log1p))
    //
    // .filter(
    //
    //  f ->
    //
    //          f
    //
    //                  .term(
    //
    //                          t ->
    //
    //                                  t
    //
    //                                          .field(
    //
    //                                                  "a")))
    //
    // .weight(
    //
    //  2.0)))),
    //                Book.class);
    //    }

    @Test
    void update() {}

    @Test
    void testUpdate() {}

    // NOTE :
    // https://github.com/elastic/elasticsearch-java/blob/main/examples/esql-article/src/main/java/co/elastic/clients/esql/article/EsqlArticle.java
    //    @Test
    //    void indices() throws IOException {
    //        client.indices()
    //                .create(
    //                        c ->
    //                                c.index("books")
    //                                        .mappings(
    //                                                mp ->
    //                                                        mp.properties("title", p -> p.text(t
    // -> t))
    //                                                                .properties(
    //                                                                        "description",
    //                                                                        p -> p.text(t -> t))
    //                                                                .properties(
    //                                                                        "author",
    //                                                                        p -> p.text(t -> t))
    //                                                                .properties(
    //                                                                        "year",
    //                                                                        p -> p.short_(s -> s))
    //                                                                .properties(
    //                                                                        "publisher",
    //                                                                        p -> p.text(t -> t))
    //                                                                .properties(
    //                                                                        "ratings",
    //                                                                        p ->
    //                                                                                p.halfFloat(
    //                                                                                        hf ->
    //
    //  hf))));
    //    }
}
