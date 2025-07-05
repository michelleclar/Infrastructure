package org.carl.infrastructure.discover;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ServiceOptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ServiceLifecycle {

    @ConfigProperty(name = "quarkus.application.name")
    String appName;

    private int port;

    private final Logger logger;
    private final Instance<ConsulClient> consulClient;
    private final ScheduledExecutorService executor;

    public ServiceLifecycle(
            Logger logger, Instance<ConsulClient> consulClient, ScheduledExecutorService executor) {
        this.logger = logger;
        this.consulClient = consulClient;
        this.executor = executor;
    }

    void onStart(@Observes StartupEvent ev) {
        if (consulClient.isResolvable()) {
            executor.schedule(
                    () -> {
                        port =
                                ConfigProvider.getConfig()
                                        .getValue("quarkus.http.port", Integer.class);
                        consulClient.get().putValue("config", appName);
                        consulClient
                                .get()
                                .registerService(
                                        new ServiceOptions()
                                                .setPort(port)
                                                .setAddress("localhost")
                                                .setName(appName)
                                                .setId(appName + "-" + port),
                                        result ->
                                                logger.infof(
                                                        "Service %s-%d registered", appName, port));
                    },
                    3000,
                    TimeUnit.MILLISECONDS);
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (consulClient.isResolvable()) {
            consulClient
                    .get()
                    .deregisterService(
                            appName + "-" + port,
                            result -> logger.infof("Service %s-%d deregistered", appName, port));
        }
    }
}
