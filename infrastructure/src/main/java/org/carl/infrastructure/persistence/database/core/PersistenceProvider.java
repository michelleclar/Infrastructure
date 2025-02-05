package org.carl.infrastructure.persistence.database.core;

import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.IPersistenceProvider;

public class PersistenceProvider implements IPersistenceProvider {

    private DSLContext dslContext;

    @Override
    public DSLContext getDSLContext() {
        return dslContext;
    }

    @Inject
    @Override
    public void setDSLContext(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void resetDBInfo() {}
}
