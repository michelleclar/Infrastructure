package org.carl.infrastructure.persistence.core;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.DefaultBean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.carl.infrastructure.persistence.engine.runtime.DslContextFactory;
import org.jooq.DSLContext;

public class DSLContextProvider implements Provider<PersistenceContext> {
    @Inject AgroalDataSource dataSource;

    @Produces
    @DefaultBean
    @ApplicationScoped
    @Override
    public PersistenceContext get() {
        return PersistenceContext.create(dataSource);
    }

    @Produces
    @DefaultBean
    @ApplicationScoped
    public DSLContext getDSLContext() {
        return DslContextFactory.create(dataSource);
    }
}
