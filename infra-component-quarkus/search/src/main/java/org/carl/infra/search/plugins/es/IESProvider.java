package org.carl.infra.search.plugins.es;

import org.carl.infra.search.plugins.es.core.ESContext;

public interface IESProvider {

    ESContext getESContext();

    void setSearchDSLContext(ESContext ESClient);
}
