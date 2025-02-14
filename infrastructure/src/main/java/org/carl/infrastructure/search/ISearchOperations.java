package org.carl.infrastructure.search;

public interface ISearchOperations extends ISearchProvider {

    // core method all is ·xxxCtx·
    default SearchClient searchCtx() {
        return getSearchClient();
    }
}
