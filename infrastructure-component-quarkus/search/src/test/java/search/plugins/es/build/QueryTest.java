package search.plugins.es.build;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.carl.infrastructure.search.plugins.es.build.Mapping;
import org.carl.infrastructure.search.plugins.es.build.PropertyType;
import org.carl.infrastructure.search.plugins.es.build.Query;
import org.junit.jupiter.api.Test;

class QueryTest {

    @Test
    void q() throws JsonProcessingException {
        Query build =
                Query.Q()
                        .MultiMatchQueryBuild()
                        .setQuery("sadasd")
                        .setFields("content")
                        .build()
                        .TermQueryBuild()
                        .setTermPair("type", "POST")
                        .build();
        System.out.println(build);
    }

    @Test
    void mapping() {
        Mapping build =
                Mapping.build()
                        .properties(p -> p.setType(PropertyType.KEYWORD).setName("id").build())
                        .properties(p -> p.setName("type").setType(PropertyType.KEYWORD).build())
                        .properties(p -> p.setName("content").setType(PropertyType.TEXT).build());
        System.out.println(build);
    }
}
