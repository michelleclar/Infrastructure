package org.carl.infra.discover;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Locates healthy service instances and exposes subsequent instance-list changes. */
public interface ServiceDiscovery {

    CompletionStage<List<ServiceInstance>> discover(ServiceQuery query);

    Flow.Publisher<ServiceInstancesChanged> changes(ServiceQuery query);
}
