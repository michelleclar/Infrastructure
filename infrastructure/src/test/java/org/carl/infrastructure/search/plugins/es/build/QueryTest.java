package org.carl.infrastructure.search.plugins.es.build;

import com.fasterxml.jackson.core.JsonProcessingException;

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
                        .properties()
                        .setType(PropertyType.KEYWORD)
                        .setName("id")
                        .build()
                        .properties()
                        .setName("type")
                        .setType(PropertyType.KEYWORD)
                        .build()
                        .properties()
                        .setName("content")
                        .setType(PropertyType.TEXT)
                        .build();
        System.out.println(build);
    }
}
