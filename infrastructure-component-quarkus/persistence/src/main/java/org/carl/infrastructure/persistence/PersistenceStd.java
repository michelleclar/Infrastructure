package org.carl.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.carl.infrastructure.persistence.metadata.DBInfo;

@ApplicationScoped
public class PersistenceStd implements IPersistenceProvider {

    private PersistenceContext persistenceContext;
    private volatile DBInfo dbInfo;

    @Override
    public PersistenceContext dsl() {
        return persistenceContext;
    }

    @Inject
    @Override
    public void setPersistenceContext(PersistenceContext persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    @Override
    public void resetDBInfo() {
        this.dbInfo = null;
    }

    @Override
    public DBInfo getDBInfo() {
        DBInfo current = dbInfo;
        if (current == null) {
            current = IPersistenceProvider.super.getDBInfo();
            dbInfo = current;
        }
        return current;
    }
}
