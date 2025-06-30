package org.carl.infrastructure.persistence;

import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

@Singleton
@IfBuildProperty(name = "quarkus.plugins.persistence.enable", stringValue = "true")
public class PersistenceService extends PersistenceStd implements IPersistenceOperations {
    private static final Logger log = Logger.getLogger(PersistenceService.class);
}
