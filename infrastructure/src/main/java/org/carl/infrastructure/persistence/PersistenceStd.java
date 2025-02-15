package org.carl.infrastructure.persistence;

import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.core.PersistenceContext;

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
