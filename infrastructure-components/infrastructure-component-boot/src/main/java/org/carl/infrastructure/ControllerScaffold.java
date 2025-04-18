package org.carl.infrastructure;

import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.carl.infrastructure.ability.IBroadcastAbility;
import org.carl.infrastructure.ability.IRuntimeAbility;
import org.carl.infrastructure.ability.ISearchAbility;
import org.carl.infrastructure.broadcast.IBroadcastOperations;
import org.carl.infrastructure.search.plugins.es.IESOperations;
import org.jboss.logging.Logger;

/**
 * why use set inject bean because I think better copy by this write
 *
 * <p>this is platform sport controller scaffold
 *
 * <p>serchAbility , broadcast,runtime ctx
 */
public abstract class ControllerScaffold
        implements IRuntimeAbility, ISearchAbility, IBroadcastAbility {

    static final Logger log = Logger.getLogger(ControllerScaffold.class);
    private IESOperations searchOperations;
    private RoutingContext routingContext;

    @Inject
    public void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    @Inject
    public void setSearchOperations(IESOperations searchOperations) {
        this.searchOperations = searchOperations;
    }

    protected void beforeInit() {}

    @PostConstruct
    protected void init() {
        afterInit();
        log.infof("{} initialized", this.getClass().getSimpleName());
    }

    protected void afterInit() {}

    @Override
    public IBroadcastOperations getBroadcastOperations() {
        return null;
    }

    @Override
    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    @Override
    public IESOperations getSearchOperations() {
        return searchOperations;
    }
}
