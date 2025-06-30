package org.carl.infrastructure.persistence.core;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.DefaultBean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.carl.infrastructure.persistence.engine.runtime.DslContextFactory;
import org.jooq.DSLContext;

public class DSLContextProvider implements Provider<PersistenceContext> {
    @Inject AgroalDataSource dataSource;

    @ApplicationScoped
    @DefaultBean
    @Override
    public PersistenceContext get() {
        return PersistenceContext.create(dataSource);
    }

    @ApplicationScoped
    @DefaultBean
    public DSLContext getDSLContext() {
        return DslContextFactory.create(dataSource);
    }
}
