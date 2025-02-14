package org.carl.infrastructure.search;

import jakarta.inject.Inject;

public class SearchStd implements ISearchProvider {
    private SearchClient client;


    @Override
    public SearchClient getSearchClient() {
        return client;
    }

    @Inject
    @Override
    public void setSearchDSLContext(SearchClient searchClient) {
        this.client = searchClient;
    }
}
