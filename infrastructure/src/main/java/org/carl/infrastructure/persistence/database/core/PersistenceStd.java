package org.carl.infrastructure.persistence.database.core;

import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.IPersistenceProvider;

public class PersistenceStd implements IPersistenceProvider {

    private PersistenceContext persistenceContext;

    @Override
    public PersistenceContext getPersistenceContext() {
        return persistenceContext;
    }

    @Inject
    @Override
    public void setPersistenceContext(PersistenceContext persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    @Override
    public void resetDBInfo() {}
}
