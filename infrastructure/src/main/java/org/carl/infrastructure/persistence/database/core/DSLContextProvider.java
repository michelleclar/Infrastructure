package org.carl.infrastructure.persistence.database.core;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.carl.infrastructure.persistence.engine.runtime.DslContextFactory;

@ApplicationScoped
public class DSLContextProvider implements Provider<DSLContext> {
    @Inject AgroalDataSource dataSource;

    @Produces
    @ApplicationScoped
    @Override
    public DSLContext get() {
        return DSLContext.create(DslContextFactory.create(dataSource));
    }
}
