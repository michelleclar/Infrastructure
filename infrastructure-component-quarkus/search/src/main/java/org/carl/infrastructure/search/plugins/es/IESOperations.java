package org.carl.infrastructure.search.plugins.es;

import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;

import org.carl.infrastructure.search.plugins.es.core.action.Delete;
import org.carl.infrastructure.search.plugins.es.core.action.Get;
import org.carl.infrastructure.search.plugins.es.core.action.Index;
import org.carl.infrastructure.search.plugins.es.core.action.Search;
import org.carl.infrastructure.search.plugins.es.core.action.Update;

public interface IESOperations extends IESProvider {

    default <T> Index<T> index(IndexRequest.Builder<T> builder) {
        return new Index<>(getESContext().getClient(), builder);
    }

    default Search search(SearchRequest.Builder builder) {
        return new Search(getESContext().getClient(), builder);
    }

    default Get get(GetRequest.Builder builder) {
        return new Get(getESContext().getClient(), builder);
    }

    default <T, K> Update<T, K> update(UpdateRequest.Builder<T, K> builder) {
        return new Update<>(getESContext().getClient(), builder);
    }

    default Delete delete(DeleteRequest.Builder builder) {
        return new Delete(getESContext().getClient(), builder);
    }
}
