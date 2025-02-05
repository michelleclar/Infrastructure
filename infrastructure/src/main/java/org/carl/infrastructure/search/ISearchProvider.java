package org.carl.infrastructure.search;

public interface ISearchProvider {

    SearchClient getSearchClient();

    void setDSLContext(SearchClient searchClient);

    void resetDBInfo();
}
