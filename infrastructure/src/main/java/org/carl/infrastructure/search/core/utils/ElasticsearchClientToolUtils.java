package org.carl.infrastructure.search.core.utils;

import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.ArrayList;
import java.util.List;

public class ElasticsearchClientToolUtils {
    public static <T> List<T> hitsToList(List<Hit<T>> hitList) {
        ArrayList<T> r = new ArrayList<>(hitList.size());
        for (Hit<T> hit : hitList) {
            T source = hit.source();
            r.add(source);
        }
        return r;
    }
}
