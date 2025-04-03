package org.carl.infrastructure.persistence.core;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.carl.infrastructure.persistence.engine.runtime.DslContextFactory;
import org.jooq.DSLContext;

public class DSLContextProvider implements Provider<PersistenceContext> {
    @Inject AgroalDataSource dataSource;

    @ApplicationScoped
    @Override
    public PersistenceContext get() {
        return PersistenceContext.create(DslContextFactory.create(dataSource));
    }

    @ApplicationScoped
    public DSLContext dslContext() {
        return DslContextFactory.create(dataSource);
    }
}
