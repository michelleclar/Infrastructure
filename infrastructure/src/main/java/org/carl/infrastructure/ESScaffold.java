package org.carl.infrastructure;

import jakarta.annotation.PostConstruct;

import org.carl.infrastructure.search.plugins.es.ESStd;
import org.jboss.logging.Logger;

/** why use set inject bean because I think better copy by this write */
public abstract class ESScaffold extends ESStd {

    static final Logger log = Logger.getLogger(ESScaffold.class);

    @PostConstruct
    protected void init() {}

    protected void afterInit() {}
}
