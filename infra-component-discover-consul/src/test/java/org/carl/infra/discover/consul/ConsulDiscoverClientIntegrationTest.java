package org.carl.infra.discover.consul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;

import org.carl.infra.discover.DynamicConfigChanged;
import org.carl.infra.discover.HttpHealthCheck;
import org.carl.infra.discover.ServiceInstancesChanged;
import org.carl.infra.discover.ServiceQuery;
import org.carl.infra.discover.ServiceRegistration;
import org.carl.infra.discover.TcpHealthCheck;
import org.carl.infra.discover.TtlHealthCheck;
import org.junit.jupiter.api.Test;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

@Testcontainers(disabledWithoutDocker = true)
class ConsulDiscoverClientIntegrationTest {

    @Container
    private static final ConsulContainer CONSUL =
            new ConsulContainer("hashicorp/consul:1.20");

    @Test
    void registersDiscoversAndHotReloadsConfiguration() throws Exception {
        URI consulUri = consulUri();
        put(consulUri, "config/orders", "limit=1\nfeature.enabled=true\n");

        ConsulDiscoverOptions options =
                ConsulDiscoverOptions.builder(consulUri, "config/orders")
                        .connectTimeout(Duration.ofSeconds(2))
                        .blockingWait(Duration.ofSeconds(2))
                        .addValidator(snapshot -> snapshot.get("limit", Integer.class))
                        .build();

        ConsulDiscoverClient client = ConsulDiscoverClient.create(options);
        try {
            client.start().toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertTrue(client.isReady());
            assertEquals(1, client.current().get("limit", Integer.class));

            OneEventSubscriber<ServiceInstancesChanged> serviceSubscriber =
                    new OneEventSubscriber<>();
            client.changes(new ServiceQuery("orders")).subscribe(serviceSubscriber);

            ServiceRegistration registration =
                    new ServiceRegistration(
                            "orders",
                            "orders-test-1",
                            "127.0.0.1",
                            18080,
                            List.of("integration"),
                            Map.of("zone", "test"),
                            new TtlHealthCheck(Duration.ofSeconds(10), Duration.ofMinutes(1)));
            client.register(registration).toCompletableFuture().get(5, TimeUnit.SECONDS);
            client.passTtl(registration.instanceId(), "integration test")
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);

            ServiceInstancesChanged serviceChange =
                    serviceSubscriber.event.get(5, TimeUnit.SECONDS);
            assertEquals(1, serviceChange.current().size());
            assertEquals("orders-test-1", serviceChange.current().getFirst().instanceId());
            assertEquals(
                    1,
                    client.discover(new ServiceQuery("orders"))
                            .toCompletableFuture()
                            .get(5, TimeUnit.SECONDS)
                            .size());

            OneEventSubscriber<DynamicConfigChanged> configSubscriber =
                    new OneEventSubscriber<>();
            client.changes().subscribe(configSubscriber);

            put(consulUri, "config/orders", "limit=invalid\n");
            Thread.sleep(500);
            put(consulUri, "config/orders", "limit=3\nfeature.enabled=false\n");

            DynamicConfigChanged configChange =
                    configSubscriber.event.get(5, TimeUnit.SECONDS);
            assertEquals(1, configChange.previous().get("limit", Integer.class));
            assertEquals(3, configChange.current().get("limit", Integer.class));
            assertEquals(
                    Set.of("feature.enabled", "limit"),
                    configChange.changedKeys());
        } finally {
            client.close();
        }
    }

    @Test
    void supportsHealthFilteringDeregistrationAndExternalVertxOwnership() throws Exception {
        URI consulUri = consulUri();
        put(consulUri, "config/health-checks", "enabled=true\n");
        Vertx externalVertx = Vertx.vertx();
        ConsulDiscoverClient client =
                ConsulDiscoverClient.create(
                        externalVertx,
                        ConsulDiscoverOptions.builder(consulUri, "config/health-checks")
                                .connectTimeout(Duration.ofSeconds(2))
                                .blockingWait(Duration.ofSeconds(2))
                                .build());
        CompletionSubscriber<DynamicConfigChanged> completionSubscriber =
                new CompletionSubscriber<>();
        client.changes().subscribe(completionSubscriber);
        try {
            client.start().toCompletableFuture().get(5, TimeUnit.SECONDS);

            ServiceRegistration http =
                    registration(
                            "health-http",
                            "health-http-1",
                            new HttpHealthCheck(
                                    URI.create(
                                            "http://127.0.0.1:8500/v1/status/leader"),
                                    Duration.ofSeconds(1),
                                    Duration.ofMinutes(1)));
            ServiceRegistration tcp =
                    registration(
                            "health-tcp",
                            "health-tcp-1",
                            new TcpHealthCheck(
                                    "127.0.0.1:8500",
                                    Duration.ofSeconds(1),
                                    Duration.ofMinutes(1)));
            ServiceRegistration ttl =
                    registration(
                            "health-ttl",
                            "health-ttl-1",
                            new TtlHealthCheck(
                                    Duration.ofSeconds(10),
                                    Duration.ofMinutes(1)));
            ServiceRegistration failingTtl =
                    registration(
                            "health-failing-ttl",
                            "health-failing-ttl-1",
                            new TtlHealthCheck(
                                    Duration.ofSeconds(10),
                                    Duration.ofMinutes(1)));

            client.register(http).toCompletableFuture().get(5, TimeUnit.SECONDS);
            client.register(tcp).toCompletableFuture().get(5, TimeUnit.SECONDS);
            client.register(ttl).toCompletableFuture().get(5, TimeUnit.SECONDS);
            client.register(failingTtl).toCompletableFuture().get(5, TimeUnit.SECONDS);

            await(Duration.ofSeconds(10), () -> discoveredCount(client, "health-http") == 1);
            await(Duration.ofSeconds(10), () -> discoveredCount(client, "health-tcp") == 1);
            assertEquals(0, discoveredCount(client, "health-ttl"));
            assertEquals(0, discoveredCount(client, "health-failing-ttl"));

            client.passTtl(ttl.instanceId(), null)
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            await(Duration.ofSeconds(5), () -> discoveredCount(client, "health-ttl") == 1);

            client.deregister(http.instanceId())
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            await(Duration.ofSeconds(5), () -> discoveredCount(client, "health-http") == 0);
            assertEquals(200, agentServiceStatus(consulUri, tcp.instanceId()));

            client.close();
            completionSubscriber.completed.get(5, TimeUnit.SECONDS);
            await(
                    Duration.ofSeconds(5),
                    () -> agentServiceStatus(consulUri, tcp.instanceId()) == 404);

            CompletableFuture<Void> externalVertxStillRunning = new CompletableFuture<>();
            externalVertx.setTimer(1, ignored -> externalVertxStillRunning.complete(null));
            externalVertxStillRunning.get(5, TimeUnit.SECONDS);
            assertFalse(externalVertxStillRunning.isCompletedExceptionally());
        } finally {
            client.close();
            externalVertx.close().toCompletionStage().toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void retainsConfigurationWhileConsulIsUnavailableAndReloadsAfterRecovery()
            throws Exception {
        URI consulUri = consulUri();
        put(consulUri, "config/recovery", "limit=1\n");
        ConsulDiscoverClient client =
                ConsulDiscoverClient.create(
                        ConsulDiscoverOptions.builder(consulUri, "config/recovery")
                                .connectTimeout(Duration.ofSeconds(1))
                                .blockingWait(Duration.ofSeconds(1))
                                .addValidator(snapshot -> snapshot.get("limit", Integer.class))
                                .build());
        OneEventSubscriber<DynamicConfigChanged> subscriber = new OneEventSubscriber<>();
        AtomicBoolean paused = new AtomicBoolean();
        try {
            client.start().toCompletableFuture().get(5, TimeUnit.SECONDS);
            client.changes().subscribe(subscriber);

            CONSUL.getDockerClient()
                    .pauseContainerCmd(CONSUL.getContainerId())
                    .exec();
            paused.set(true);
            Thread.sleep(4_000);
            assertEquals(1, client.current().get("limit", Integer.class));

            CONSUL.getDockerClient()
                    .unpauseContainerCmd(CONSUL.getContainerId())
                    .exec();
            paused.set(false);
            put(consulUri, "config/recovery", "limit=2\n");

            DynamicConfigChanged change = subscriber.event.get(10, TimeUnit.SECONDS);
            assertEquals(1, change.previous().get("limit", Integer.class));
            assertEquals(2, change.current().get("limit", Integer.class));
        } finally {
            if (paused.get()) {
                CONSUL.getDockerClient()
                        .unpauseContainerCmd(CONSUL.getContainerId())
                        .exec();
            }
            client.close();
        }
    }

    private static ServiceRegistration registration(
            String serviceName,
            String instanceId,
            org.carl.infra.discover.HealthCheck healthCheck) {
        return new ServiceRegistration(
                serviceName,
                instanceId,
                "127.0.0.1",
                18080,
                List.of(),
                Map.of(),
                healthCheck);
    }

    private static int discoveredCount(ConsulDiscoverClient client, String serviceName) {
        try {
            return client.discover(new ServiceQuery(serviceName))
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS)
                    .size();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static void await(Duration timeout, BooleanSupplier condition)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        assertTrue(condition.getAsBoolean(), "condition was not met within " + timeout);
    }

    private static int agentServiceStatus(URI consulUri, String instanceId) {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder(
                                    consulUri.resolve(
                                            "/v1/agent/service/" + instanceId))
                            .GET()
                            .build();
            return HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static URI consulUri() {
        return URI.create(
                "http://" + CONSUL.getHost() + ":" + CONSUL.getMappedPort(8500));
    }

    private static void put(URI consulUri, String key, String value) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(consulUri.resolve("/v1/kv/" + key))
                        .PUT(HttpRequest.BodyPublishers.ofString(value))
                        .build();
        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("true", response.body());
    }

    private static final class OneEventSubscriber<T> implements Flow.Subscriber<T> {

        private final CompletableFuture<T> event = new CompletableFuture<>();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(1);
        }

        @Override
        public void onNext(T item) {
            event.complete(item);
        }

        @Override
        public void onError(Throwable throwable) {
            event.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            if (!event.isDone()) {
                event.completeExceptionally(
                        new IllegalStateException("Publisher closed before an event arrived"));
            }
        }
    }

    private static final class CompletionSubscriber<T> implements Flow.Subscriber<T> {

        private final CompletableFuture<Void> completed = new CompletableFuture<>();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T item) {
        }

        @Override
        public void onError(Throwable throwable) {
            completed.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            completed.complete(null);
        }
    }
}
