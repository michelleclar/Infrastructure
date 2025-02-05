package org.carl.infrastructure;

import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.carl.infrastructure.ability.IRuntimeAbility;
import org.carl.infrastructure.ability.ISearchAbility;
import org.carl.infrastructure.ability.ISentMessageAbility;
import org.carl.infrastructure.persistence.IPersistenceOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Scaffold implements IPersistenceOperations, IRuntimeAbility, ISearchAbility, ISentMessageAbility {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private IPersistenceOperations persistenceOperations;
    private EventBus eventBus;
    private RoutingContext routingContext;

    @Inject
    public void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    @Inject
    public void setDatabaseOperations(IPersistenceOperations databaseOperations) {
        this.persistenceOperations = databaseOperations;
    }

    @Inject
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public IPersistenceOperations getDatabaseOperations() {
        return persistenceOperations;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    @PostConstruct
    protected void init() {
        if (this instanceof ITableCreateAbility ability) {
            ability.create(getDatabaseOperations());
        }
        afterInit();
        logger.info("{} initialized", this.getClass().getSimpleName());
    }

    protected void afterInit() {

    }

}
