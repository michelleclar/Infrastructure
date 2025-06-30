package org.carl.infrastructure.search.plugins.es;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

// TODO: use json query to search ?
@ApplicationScoped
public class ESService extends ESStd implements IESOperations {
    static final Logger log = Logger.getLogger(ESService.class);
}
