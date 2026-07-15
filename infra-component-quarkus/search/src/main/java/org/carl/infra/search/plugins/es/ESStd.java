package org.carl.infra.search.plugins.es;

import jakarta.inject.Inject;

import org.carl.infra.search.plugins.es.core.ESContext;

public class ESStd implements IESProvider {

    @Inject ESContext context;

    @Override
    public ESContext getESContext() {
        return context;
    }

    @Override
    public void setSearchDSLContext(ESContext ctx) {
        this.context = ctx;
    }
}
