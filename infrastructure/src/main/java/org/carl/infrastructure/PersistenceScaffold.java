package org.carl.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.carl.infrastructure.core.ability.IPersistenceAbility;
import org.carl.infrastructure.core.ability.ITableCreateAbility;
import org.carl.infrastructure.persistence.IPersistenceOperations;
import org.jboss.logging.Logger;

/** why use set inject bean because I think better copy by this write */
public abstract class PersistenceScaffold implements IPersistenceAbility {

    static final Logger log = Logger.getLogger(PersistenceScaffold.class);
    private IPersistenceOperations persistenceOperations;

    @Inject
    public void setPersistenceOperations(IPersistenceOperations databaseOperations) {
        this.persistenceOperations = databaseOperations;
    }

    @Override
    public IPersistenceOperations getPersistenceOperations() {
        return persistenceOperations;
    }

    @PostConstruct
    protected void init() {
        if (this instanceof ITableCreateAbility ability) {
            ability.create(getPersistenceOperations());
        }
        afterInit();
        log.infof("{} initialized", this.getClass().getSimpleName());
    }

    protected void afterInit() {}
}
