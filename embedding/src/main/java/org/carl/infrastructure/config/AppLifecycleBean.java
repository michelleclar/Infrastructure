package org.carl.infrastructure.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.carl.domain.gateway.IQdrantGateway;
import org.carl.domain.qdrant.VectorCollection;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class AppLifecycleBean {
    private static final Logger LOGGER = Logger.getLogger("ListenerBean");
    @Inject IQdrantGateway qdrantGateway;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Application started");

        List<Uni<Boolean>> tasks =
                Arrays.stream(VectorCollection.values())
                        .map(
                                item ->
                                        qdrantGateway
                                                .collectionUpsert(item)
                                                .invoke(
                                                        response -> {
                                                            if (response) {
                                                                LOGGER.infof(
                                                                        "Collection [%s] is Completed",
                                                                        item);
                                                            } else {
                                                                LOGGER.errorf(
                                                                        "Failed to create collection [%s]",
                                                                        item);
                                                            }
                                                        }))
                        .toList();

        Uni.combine().all().unis(tasks).with(list -> list).await().indefinitely();

        LOGGER.info("The application is starting...");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
    }
}
