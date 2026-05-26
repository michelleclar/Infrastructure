package org.carl.infrastructure.search.plugins.es;

import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

// TODO: use json query to search ?
@ApplicationScoped
@IfBuildProperty(name = "quarkus.plugins.search.enable", stringValue = "true")
public class ESService extends ESStd implements IESOperations {
    static final Logger log = Logger.getLogger(ESService.class);
}
