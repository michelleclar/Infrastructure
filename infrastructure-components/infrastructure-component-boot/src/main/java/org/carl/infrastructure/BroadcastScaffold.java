package org.carl.infrastructure;

import jakarta.annotation.PostConstruct;
import org.carl.infrastructure.broadcast.BroadcastStd;
import org.jboss.logging.Logger;

/** why use set inject bean because I think better copy by this write */
public abstract class BroadcastScaffold extends BroadcastStd {

    static final Logger log = Logger.getLogger(BroadcastScaffold.class);

    @PostConstruct
    protected void init() {
        afterInit();
        log.infof("{} initialized", this.getClass().getSimpleName());
    }

    protected void afterInit() {}
}
