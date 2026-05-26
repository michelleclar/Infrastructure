package org.carl.infrastructure.broadcast;

import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProperty(name = "quarkus.plugins.broadcast.enable", stringValue = "true")
public class BroadcastService extends BroadcastStd implements IBroadcastOperations {

}
