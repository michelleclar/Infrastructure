package org.carl.infrastructure.search.ability;

import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;

import org.carl.infrastructure.search.plugins.es.IESOperations;
import org.carl.infrastructure.search.plugins.es.core.action.Delete;
import org.carl.infrastructure.search.plugins.es.core.action.Get;
import org.carl.infrastructure.search.plugins.es.core.action.Index;
import org.carl.infrastructure.search.plugins.es.core.action.Search;
import org.carl.infrastructure.search.plugins.es.core.action.Update;

public interface ISearchAbility {
    IESOperations getSearchOperations();

    default <T> Index<T> index(IndexRequest.Builder<T> builder) {
        return getSearchOperations().index(builder);
    }

    default Search search(SearchRequest.Builder builder) {
        return getSearchOperations().search(builder);
    }

    default Get get(GetRequest.Builder builder) {
        return getSearchOperations().get(builder);
    }

    default <T, K> Update<T, K> update(UpdateRequest.Builder<T, K> builder) {
        return getSearchOperations().update(builder);
    }

    default Delete delete(DeleteRequest.Builder builder) {
        return getSearchOperations().delete(builder);
    }
}
