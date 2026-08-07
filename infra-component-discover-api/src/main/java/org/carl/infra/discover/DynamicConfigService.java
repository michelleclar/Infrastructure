package org.carl.infra.discover;

import java.util.concurrent.Flow;

/** Provides an immutable runtime-configuration snapshot and its changes. */
public interface DynamicConfigService {

    DynamicConfigSnapshot current();

    Flow.Publisher<DynamicConfigChanged> changes();
}
