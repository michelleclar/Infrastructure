package org.carl.infrastructure.search.plugins.es;

import org.carl.infrastructure.search.plugins.es.core.ESContext;

public interface IESProvider {

    ESContext getESContext();

    void setSearchDSLContext(ESContext ESClient);
}
