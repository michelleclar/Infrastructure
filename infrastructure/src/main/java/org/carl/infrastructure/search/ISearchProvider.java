package org.carl.infrastructure.search;

public interface ISearchProvider {

    SearchClient getSearchClient();

    void setSearchDSLContext(SearchClient searchClient);
}
