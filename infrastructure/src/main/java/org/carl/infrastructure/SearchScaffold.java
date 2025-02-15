package org.carl.infrastructure;

import jakarta.annotation.PostConstruct;

import org.carl.infrastructure.search.SearchStd;
import org.jboss.logging.Logger;

/** why use set inject bean because I think better copy by this write */
public abstract class SearchScaffold extends SearchStd {

    static final Logger log = Logger.getLogger(SearchScaffold.class);

    @PostConstruct
    protected void init() {}

    protected void afterInit() {}
}
