package org.carl.infrastructure.persistence.core;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.carl.infrastructure.persistence.engine.runtime.DslContextFactory;

public class DSLContextProvider implements Provider<PersistenceContext> {
    @Inject AgroalDataSource dataSource;

    @Produces
    @ApplicationScoped
    @Override
    public PersistenceContext get() {
        return PersistenceContext.create(DslContextFactory.create(dataSource));
    }
}
