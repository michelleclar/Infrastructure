package org.carl.infrastructure;

import jakarta.annotation.PostConstruct;

import org.carl.infrastructure.cache.CacheStd;
import org.jboss.logging.Logger;

/** why use set inject bean because I think better copy by this write */
public abstract class CacheScaffold extends CacheStd {

    static final Logger log = Logger.getLogger(CacheScaffold.class);

    @PostConstruct
    protected void init() {
        afterInit();
        log.infof("{} initialized", this.getClass().getSimpleName());
    }

    protected void afterInit() {}
}
