package org.carl.infra.discover;

import java.util.concurrent.CompletionStage;

/** Registers and deregisters service instances. */
public interface ServiceRegistrar {

    CompletionStage<Void> register(ServiceRegistration registration);

    CompletionStage<Void> deregister(String instanceId);
}
