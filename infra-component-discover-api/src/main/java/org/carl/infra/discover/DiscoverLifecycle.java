package org.carl.infra.discover;

import java.util.concurrent.CompletionStage;

/** Lifecycle contract shared by discovery implementations. */
public interface DiscoverLifecycle extends AutoCloseable {

    CompletionStage<Void> start();

    boolean isReady();

    @Override
    void close();
}
