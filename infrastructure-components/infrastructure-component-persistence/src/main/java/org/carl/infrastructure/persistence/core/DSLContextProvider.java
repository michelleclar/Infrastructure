package org.carl.infrastructure.persistence.core;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class DSLContextProvider implements Provider<PersistenceContext> {
    @Inject AgroalDataSource dataSource;

    @ApplicationScoped
    @DefaultBean
    @Override
    public PersistenceContext get() {
        return PersistenceContext.create(dataSource);
    }
}
