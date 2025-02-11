package org.carl.infrastructure;

import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.carl.infrastructure.broadcast.IBroadcastOperations;
import org.carl.infrastructure.core.ability.IBroadcastAbility;
import org.carl.infrastructure.core.ability.IRuntimeAbility;
import org.carl.infrastructure.core.ability.ISearchAbility;
import org.carl.infrastructure.search.ISearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * why use set inject bean because I think better copy by this write
 *
 * <p>this is platform sport controller scaffold
 *
 * <p>serchAbility , broadcast,runtime ctx
 */
public abstract class ControllerScaffold
        implements IRuntimeAbility, ISearchAbility, IBroadcastAbility {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private ISearchOperations searchOperations;
    private RoutingContext routingContext;

    @Inject
    public void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    @Inject
    public void setSearchOperations(ISearchOperations searchOperations) {
        this.searchOperations = searchOperations;
    }

    protected void beforeInit() {}

    @PostConstruct
    protected void init() {
        afterInit();
        logger.info("{} initialized", this.getClass().getSimpleName());
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
    public ISearchOperations getSearchOperations() {
        return searchOperations;
    }
}
