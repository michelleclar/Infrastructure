package org.carl.infrastructure;

import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.carl.infrastructure.broadcast.IBroadcastOperations;
import org.carl.infrastructure.core.ability.IBroadcastAbility;
import org.carl.infrastructure.core.ability.IPersistenceAbility;
import org.carl.infrastructure.core.ability.IRuntimeAbility;
import org.carl.infrastructure.core.ability.ISearchAbility;
import org.carl.infrastructure.core.ability.ITableCreateAbility;
import org.carl.infrastructure.persistence.IPersistenceOperations;
import org.carl.infrastructure.search.ISearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** why use set inject bean because I think better copy by this write */
public abstract class Scaffold
        implements IRuntimeAbility, ISearchAbility, IBroadcastAbility, IPersistenceAbility {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private IPersistenceOperations persistenceOperations;
    private ISearchOperations searchOperations;
    private EventBus eventBus;
    private RoutingContext routingContext;

    @Inject
    public void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    @Inject
    public void setPersistenceOperations(IPersistenceOperations databaseOperations) {
        this.persistenceOperations = databaseOperations;
    }

    @Inject
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Inject
    public void setSearchOperations(ISearchOperations searchOperations) {
        this.searchOperations = searchOperations;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    @PostConstruct
    protected void init() {
        if (this instanceof ITableCreateAbility ability) {
            ability.create(getPersistenceOperations());
        }
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
