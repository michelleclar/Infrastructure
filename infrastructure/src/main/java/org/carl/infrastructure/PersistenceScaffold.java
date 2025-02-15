package org.carl.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.carl.infrastructure.core.ability.ITableCreateAbility;
import org.carl.infrastructure.persistence.IPersistenceOperations;
import org.carl.infrastructure.persistence.PersistenceStd;
import org.jboss.logging.Logger;

/** why use set inject bean because I think better copy by this write */
public abstract class PersistenceScaffold extends PersistenceStd {

    static final Logger log = Logger.getLogger(PersistenceScaffold.class);
    @Inject IPersistenceOperations operations;

    @Inject
    public void setPersistenceOperations(IPersistenceOperations operations) {
        this.operations = operations;
    }

    @PostConstruct
    protected void init() {
        if (this instanceof ITableCreateAbility ability) {
            ability.create(operations);
        }
        afterInit();
        log.infof("{} initialized", this.getClass().getSimpleName());
    }

    protected void afterInit() {}
}
