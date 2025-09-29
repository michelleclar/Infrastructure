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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    void onStart(@Observes StartupEvent ev)
            throws InterruptedException, ExecutionException, TimeoutException {
        this.consulClient =
                ConsulClient.create(
                        Vertx.vertx(),
                        new ConsulClientOptions().setTimeout(5000L).setHost(host).setPort(port));

        if (logger.isDebugEnabled()) {
            this.consulClient
                    .agentInfo()
                    .andThen(
                            result -> {
                                if (result.succeeded()) {
                                    logger.debugf("Consul info: %s", result.result());
                                }
                            });
            consulClient
                    .getValues(getConfigPrefix())
                    .andThen(
                            result -> {
                                if (result.succeeded()) {
                                    result.result()
                                            .getList()
                                            .forEach(
                                                    o -> {
                                                        logger.debugf(
                                                                "Consul key: [%s],value: [%s]",
                                                                o.getKey(), o.getValue());
                                                    });
                                }
                            })
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
        }
        consulClient
                .registerService(
                        new ServiceOptions()
                                .setPort(appPort)
                                .setAddress("localhost")
                                .setName(appName)
                                .setId(getDiscoverServerName()))
                .andThen(
                        result -> {
                            if (result.succeeded()) {
                                logger.infof("Service %s-%d registered", appName, appPort);
                            } else {
                                logger.errorf(
                                        result.cause(),
                                        "Service %s-%d registered failed",
                                        appName,
                                        appPort);
                            }
                        })
                .toCompletionStage()
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev)
            throws InterruptedException, ExecutionException, TimeoutException {
        this.consulClient
                .deregisterService(getDiscoverServerName())
                .andThen(
                        result -> {
                            if (result.succeeded()) {
                                logger.infof("Service %s-%d deregistered", appName, appPort);
                            } else {
                                logger.errorf(
                                        result.cause(),
                                        "Service %s-%d deregistered failed",
                                        appName,
                                        appPort);
                            }
                        })
                .toCompletionStage()
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
    }

    public String getDiscoverServerName() {
        return appName + "-" + appPort;
    }

    public String getConfigPrefix() {
        return "config/" + appName;
    }
}
