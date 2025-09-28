package org.carl.infrastructure.discover;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ServiceLifecycle {

    @ConfigProperty(name = "consul.host")
    String host;

    @ConfigProperty(name = "consul.port")
    int port;

    @ConfigProperty(name = "quarkus.application.name")
    String appName;

    @ConfigProperty(name = "quarkus.http.port")
    int appPort;

    private final Logger logger;
    private ConsulClient consulClient;

    public ServiceLifecycle(Logger logger) {
        this.logger = logger;
    }

    void onStart(@Observes StartupEvent ev, Vertx vertx) {
        this.consulClient =
                ConsulClient.create(vertx, new ConsulClientOptions().setHost(host).setPort(port));

        consulClient.putValue("config", appName);
        consulClient.registerService(
                new ServiceOptions()
                        .setPort(appPort)
                        .setAddress("localhost")
                        .setName(appName)
                        .setId(appName + "-" + appPort),
                result -> logger.infof("Service %s-%d registered", appName, appPort));
    }

    void onStop(@Observes ShutdownEvent ev) {
        this.consulClient.deregisterService(
                appName + "-" + appPort,
                result -> {
                    if (result.succeeded()) {
                        logger.infof("Service %s-%d deregistered", appName, port);
                    } else {
                        logger.errorf("Service %s-%d deregistered failed", appName, port);
                    }
                });
    }
}
