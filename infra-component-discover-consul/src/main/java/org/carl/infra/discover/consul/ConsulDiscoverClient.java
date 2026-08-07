package org.carl.infra.discover.consul;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.BlockingQueryOptions;
import io.vertx.ext.consul.CheckOptions;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.Service;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.ext.consul.ServiceQueryOptions;

import org.carl.infra.discover.DiscoverLifecycle;
import org.carl.infra.discover.DynamicConfigChanged;
import org.carl.infra.discover.DynamicConfigService;
import org.carl.infra.discover.DynamicConfigSnapshot;
import org.carl.infra.discover.DynamicConfigValidationException;
import org.carl.infra.discover.HealthCheck;
import org.carl.infra.discover.HttpHealthCheck;
import org.carl.infra.discover.NoHealthCheck;
import org.carl.infra.discover.ServiceDiscovery;
import org.carl.infra.discover.ServiceInstance;
import org.carl.infra.discover.ServiceInstancesChanged;
import org.carl.infra.discover.ServiceQuery;
import org.carl.infra.discover.ServiceRegistrar;
import org.carl.infra.discover.ServiceRegistration;
import org.carl.infra.discover.TcpHealthCheck;
import org.carl.infra.discover.TtlHealthCheck;
import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standalone Consul implementation for registration, healthy service discovery, and dynamic
 * configuration.
 */
public final class ConsulDiscoverClient
        implements DiscoverLifecycle, ServiceDiscovery, ServiceRegistrar, DynamicConfigService {

    private static final ILogger LOGGER = LoggerFactory.getLogger(ConsulDiscoverClient.class);
    private static final Duration RETRY_INITIAL = Duration.ofSeconds(1);
    private static final Duration RETRY_MAX = Duration.ofSeconds(30);
    private static final Comparator<ServiceInstance> INSTANCE_ORDER =
            Comparator.comparing(ServiceInstance::serviceName)
                    .thenComparing(ServiceInstance::instanceId)
                    .thenComparing(ServiceInstance::address)
                    .thenComparingInt(ServiceInstance::port)
                    .thenComparing(instance -> instance.metadata().toString());

    private final Vertx vertx;
    private final boolean ownsVertx;
    private final ConsulDiscoverOptions options;
    private final ConsulClient client;
    private final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<DynamicConfigSnapshot> config =
            new AtomicReference<>(DynamicConfigSnapshot.empty());
    private final SubmissionPublisher<DynamicConfigChanged> configPublisher =
            new SubmissionPublisher<>();
    private final Map<ServiceQuery, ServiceWatch> serviceWatches = new ConcurrentHashMap<>();
    private final Set<String> registeredInstanceIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> timerIds = ConcurrentHashMap.newKeySet();

    private CompletableFuture<Void> startFuture;

    private ConsulDiscoverClient(
            Vertx vertx, boolean ownsVertx, ConsulDiscoverOptions options) {
        this.vertx = vertx;
        this.ownsVertx = ownsVertx;
        this.options = options;
        this.client = ConsulClient.create(vertx, toClientOptions(options));
    }

    public static ConsulDiscoverClient create(ConsulDiscoverOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        return new ConsulDiscoverClient(Vertx.vertx(), true, options);
    }

    public static ConsulDiscoverClient create(Vertx vertx, ConsulDiscoverOptions options) {
        if (vertx == null) {
            throw new IllegalArgumentException("vertx must not be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        return new ConsulDiscoverClient(vertx, false, options);
    }

    @Override
    public synchronized CompletionStage<Void> start() {
        if (closed.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Consul discover client is closed"));
        }
        if (startFuture != null) {
            return startFuture;
        }

        startFuture = new CompletableFuture<>();
        client.getValue(options.configKey())
                .onComplete(
                        result -> {
                            if (closed.get()) {
                                startFuture.completeExceptionally(
                                        new IllegalStateException(
                                                "Consul discover client was closed during startup"));
                                return;
                            }
                            if (result.failed()) {
                                handleInitialFailure(result.cause());
                                return;
                            }
                            KeyValue keyValue = result.result();
                            if (keyValue == null || !keyValue.isPresent()) {
                                handleInitialMissing();
                                return;
                            }
                            try {
                                applyConfig(keyValue);
                                finishStart(ConsulIndex.next(0, keyValue.getModifyIndex()));
                            } catch (RuntimeException error) {
                                handleInitialFailure(error);
                            }
                        });
        return startFuture;
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public CompletionStage<List<ServiceInstance>> discover(ServiceQuery query) {
        requireStarted();
        if (query == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("query must not be null"));
        }
        return client.healthServiceNodesWithOptions(query.serviceName(), true, queryOptions(query, 0))
                .map(ConsulDiscoverClient::mapInstances)
                .toCompletionStage();
    }

    @Override
    public Flow.Publisher<ServiceInstancesChanged> changes(ServiceQuery query) {
        requireOpen();
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        ServiceWatch watch = serviceWatches.computeIfAbsent(query, ServiceWatch::new);
        return subscriber -> {
            watch.publisher.subscribe(subscriber);
            if (ready.get()) {
                watch.start();
            }
        };
    }

    @Override
    public CompletionStage<Void> register(ServiceRegistration registration) {
        requireStarted();
        if (registration == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("registration must not be null"));
        }

        ServiceOptions serviceOptions =
                new ServiceOptions()
                        .setName(registration.serviceName())
                        .setId(registration.instanceId())
                        .setAddress(registration.address())
                        .setPort(registration.port())
                        .setTags(registration.tags())
                        .setMeta(registration.metadata());
        CheckOptions checkOptions = toCheckOptions(registration);
        if (checkOptions != null) {
            serviceOptions.setCheckOptions(checkOptions);
        }

        return client.registerService(serviceOptions)
                .onSuccess(ignored -> registeredInstanceIds.add(registration.instanceId()))
                .toCompletionStage();
    }

    @Override
    public CompletionStage<Void> deregister(String instanceId) {
        requireStarted();
        if (instanceId == null || instanceId.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("instanceId must not be blank"));
        }
        return client.deregisterService(instanceId)
                .onSuccess(ignored -> registeredInstanceIds.remove(instanceId))
                .toCompletionStage();
    }

    /** Marks the default Consul TTL check for an instance as passing. */
    public CompletionStage<Void> passTtl(String instanceId, String note) {
        requireStarted();
        if (instanceId == null || instanceId.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("instanceId must not be blank"));
        }
        String checkId = "service:" + instanceId;
        Future<Void> future =
                note == null || note.isBlank()
                        ? client.passCheck(checkId)
                        : client.passCheckWithNote(checkId, note);
        return future.toCompletionStage();
    }

    @Override
    public DynamicConfigSnapshot current() {
        return config.get();
    }

    @Override
    public Flow.Publisher<DynamicConfigChanged> changes() {
        return configPublisher;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ready.set(false);
        timerIds.forEach(vertx::cancelTimer);
        timerIds.clear();
        serviceWatches.values().forEach(ServiceWatch::close);
        configPublisher.close();

        List<Future<?>> deregistrations = new ArrayList<>();
        registeredInstanceIds.forEach(
                instanceId ->
                        deregistrations.add(
                                client.deregisterService(instanceId)
                                        .onFailure(
                                                error ->
                                                        LOGGER.warn(
                                                                "Failed to deregister Consul service "
                                                                        + instanceId,
                                                                error))));
        Future<?> cleanup =
                deregistrations.isEmpty()
                        ? Future.succeededFuture()
                        : Future.join(deregistrations);
        cleanup.onComplete(
                ignored -> {
                    registeredInstanceIds.clear();
                    client.close();
                    if (ownsVertx) {
                        vertx.close()
                                .onFailure(
                                        error ->
                                                LOGGER.warn(
                                                        "Failed to close owned Vert.x instance",
                                                        error));
                    }
                });
    }

    private void handleInitialMissing() {
        if (options.initialConfigRequired()) {
            handleInitialFailure(
                    new DynamicConfigValidationException(
                            "Required Consul configuration key is missing: "
                                    + options.configKey()));
        } else {
            finishStart(1);
        }
    }

    private void handleInitialFailure(Throwable error) {
        if (options.initialConfigRequired()) {
            startFuture.completeExceptionally(error);
            close();
        } else {
            LOGGER.warn(
                    "Initial Consul configuration is unavailable; starting with an empty snapshot",
                    error);
            finishStart(0);
        }
    }

    private void finishStart(long configIndex) {
        ready.set(true);
        startFuture.complete(null);
        watchConfig(configIndex, RETRY_INITIAL);
        serviceWatches.values().forEach(ServiceWatch::start);
    }

    private void watchConfig(long index, Duration retryDelay) {
        if (closed.get()) {
            return;
        }
        client.getValueWithOptions(
                        options.configKey(),
                        new BlockingQueryOptions()
                                .setIndex(index)
                                .setWait(toConsulDuration(options.blockingWait())))
                .onComplete(
                        result -> {
                            if (closed.get()) {
                                return;
                            }
                            if (result.failed()) {
                                LOGGER.warn(
                                        "Consul configuration watch failed; a full read will be retried",
                                        result.cause());
                                schedule(
                                        retryDelay,
                                        () -> watchConfig(0, doubled(retryDelay)));
                                return;
                            }

                            KeyValue keyValue = result.result();
                            long returnedIndex =
                                    keyValue == null ? 0 : keyValue.getModifyIndex();
                            long nextIndex = ConsulIndex.next(index, returnedIndex);
                            if (keyValue != null && keyValue.isPresent()) {
                                try {
                                    applyConfig(keyValue);
                                } catch (RuntimeException error) {
                                    LOGGER.error(
                                            "Rejected Consul configuration version "
                                                    + returnedIndex,
                                            error);
                                }
                            } else {
                                LOGGER.warn(
                                        "Consul configuration key is missing; retaining version "
                                                + config.get().version());
                            }
                            schedule(
                                    Duration.ZERO,
                                    () -> watchConfig(nextIndex, RETRY_INITIAL));
                        });
    }

    private void applyConfig(KeyValue keyValue) {
        String version = Long.toString(Math.max(1, keyValue.getModifyIndex()));
        DynamicConfigSnapshot next =
                ConsulConfigParser.parse(
                        keyValue.getValue(), version, options.validators());
        DynamicConfigSnapshot previous = config.get();
        if (previous.version().equals(next.version())) {
            return;
        }
        config.set(next);
        if (!previous.values().equals(next.values())) {
            configPublisher.submit(
                    new DynamicConfigChanged(previous, next, changedKeys(previous, next)));
        }
    }

    private Set<String> changedKeys(
            DynamicConfigSnapshot previous, DynamicConfigSnapshot current) {
        Set<String> keys = new TreeSet<>();
        keys.addAll(previous.values().keySet());
        keys.addAll(current.values().keySet());
        keys.removeIf(
                key ->
                        java.util.Objects.equals(
                                previous.values().get(key), current.values().get(key)));
        return keys;
    }

    static List<ServiceInstance> mapInstances(ServiceEntryList entries) {
        List<ServiceInstance> discovered = new ArrayList<>();
        if (entries == null || entries.getList() == null) {
            return List.of();
        }
        for (ServiceEntry entry : entries.getList()) {
            Service service = entry.getService();
            if (service == null || service.getId() == null || service.getId().isBlank()) {
                continue;
            }
            String address = service.getAddress();
            if ((address == null || address.isBlank()) && entry.getNode() != null) {
                address = entry.getNode().getAddress();
            }
            if (address == null || address.isBlank() || service.getPort() < 1) {
                LOGGER.warn("Ignoring Consul service without a usable address: " + service.getId());
                continue;
            }
            ServiceInstance instance =
                    new ServiceInstance(
                            service.getName(),
                            service.getId(),
                            address,
                            service.getPort(),
                            service.getMeta());
            discovered.add(instance);
        }
        discovered.sort(INSTANCE_ORDER);
        Map<String, ServiceInstance> unique = new LinkedHashMap<>();
        discovered.forEach(instance -> unique.putIfAbsent(instance.instanceId(), instance));
        return List.copyOf(unique.values());
    }

    private ServiceQueryOptions queryOptions(ServiceQuery query, long index) {
        ServiceQueryOptions queryOptions =
                new ServiceQueryOptions()
                        .setBlockingOptions(
                                new BlockingQueryOptions()
                                        .setIndex(index)
                                        .setWait(
                                                toConsulDuration(options.blockingWait())));
        if (!query.tag().isBlank()) {
            queryOptions.setTag(query.tag());
        }
        return queryOptions;
    }

    static CheckOptions toCheckOptions(ServiceRegistration registration) {
        HealthCheck healthCheck = registration.healthCheck();
        if (healthCheck instanceof NoHealthCheck) {
            return null;
        }
        CheckOptions result =
                new CheckOptions()
                        .setId("service:" + registration.instanceId())
                        .setName(registration.serviceName() + " health")
                        .setDeregisterAfter(deregisterAfter(healthCheck));
        if (healthCheck instanceof HttpHealthCheck http) {
            return result.setHttp(http.uri().toString())
                    .setInterval(toConsulDuration(http.interval()))
                    .setTlsSkipVerify(http.tlsSkipVerify())
                    .setHeaders(http.headers());
        }
        if (healthCheck instanceof TcpHealthCheck tcp) {
            return result.setTcp(tcp.target())
                    .setInterval(toConsulDuration(tcp.interval()));
        }
        if (healthCheck instanceof TtlHealthCheck ttl) {
            return result.setTtl(toConsulDuration(ttl.ttl()));
        }
        throw new IllegalArgumentException(
                "Unsupported health check type: " + healthCheck.getClass().getName());
    }

    private static String deregisterAfter(HealthCheck healthCheck) {
        if (healthCheck instanceof HttpHealthCheck http) {
            return toConsulDuration(http.deregisterAfter());
        }
        if (healthCheck instanceof TcpHealthCheck tcp) {
            return toConsulDuration(tcp.deregisterAfter());
        }
        if (healthCheck instanceof TtlHealthCheck ttl) {
            return toConsulDuration(ttl.deregisterAfter());
        }
        throw new IllegalArgumentException(
                "Health check does not define deregisterAfter: "
                        + healthCheck.getClass().getName());
    }

    private void schedule(Duration delay, Runnable action) {
        if (closed.get()) {
            return;
        }
        long timerId =
                vertx.setTimer(
                        Math.max(1, delay.toMillis()),
                        id -> {
                            timerIds.remove(id);
                            if (!closed.get()) {
                                action.run();
                            }
                        });
        timerIds.add(timerId);
    }

    private Duration doubled(Duration value) {
        Duration doubled = value.multipliedBy(2);
        return doubled.compareTo(RETRY_MAX) > 0 ? RETRY_MAX : doubled;
    }

    private void requireStarted() {
        requireOpen();
        if (!ready.get()) {
            throw new IllegalStateException("Consul discover client has not started");
        }
    }

    private void requireOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Consul discover client is closed");
        }
    }

    private static ConsulClientOptions toClientOptions(ConsulDiscoverOptions options) {
        int port =
                options.consulUri().getPort() > 0
                        ? options.consulUri().getPort()
                        : ("https".equalsIgnoreCase(options.consulUri().getScheme()) ? 8501 : 8500);
        long requestTimeout =
                options.blockingWait().plus(options.connectTimeout()).plusSeconds(1).toMillis();
        ConsulClientOptions clientOptions =
                new ConsulClientOptions()
                        .setHost(options.consulUri().getHost())
                        .setPort(port)
                        .setSsl("https".equalsIgnoreCase(options.consulUri().getScheme()))
                        .setConnectTimeout(Math.toIntExact(options.connectTimeout().toMillis()))
                        .setTimeout(requestTimeout);
        if (!options.aclToken().isBlank()) {
            clientOptions.setAclToken(options.aclToken());
        }
        if (!options.datacenter().isBlank()) {
            clientOptions.setDc(options.datacenter());
        }
        return clientOptions;
    }

    static String toConsulDuration(Duration duration) {
        long millis = duration.toMillis();
        if (millis % Duration.ofHours(1).toMillis() == 0) {
            return (millis / Duration.ofHours(1).toMillis()) + "h";
        }
        if (millis % Duration.ofMinutes(1).toMillis() == 0) {
            return (millis / Duration.ofMinutes(1).toMillis()) + "m";
        }
        return millis % 1000 == 0 ? (millis / 1000) + "s" : millis + "ms";
    }

    private final class ServiceWatch {

        private final ServiceQuery query;
        private final SubmissionPublisher<ServiceInstancesChanged> publisher =
                new SubmissionPublisher<>();
        private final AtomicBoolean running = new AtomicBoolean();
        private final AtomicReference<List<ServiceInstance>> instances =
                new AtomicReference<>(List.of());

        private ServiceWatch(ServiceQuery query) {
            this.query = query;
        }

        private void start() {
            if (running.compareAndSet(false, true)) {
                poll(0, RETRY_INITIAL);
            }
        }

        private void poll(long index, Duration retryDelay) {
            if (closed.get() || !running.get()) {
                return;
            }
            client.healthServiceNodesWithOptions(
                            query.serviceName(), true, queryOptions(query, index))
                    .onComplete(
                            result -> {
                                if (closed.get() || !running.get()) {
                                    return;
                                }
                                if (result.failed()) {
                                    LOGGER.warn(
                                            "Consul service watch failed for "
                                                    + query.serviceName()
                                                    + "; a full read will be retried",
                                            result.cause());
                                    schedule(
                                            retryDelay,
                                            () -> poll(0, doubled(retryDelay)));
                                    return;
                                }
                                ServiceEntryList entries = result.result();
                                long returnedIndex =
                                        entries == null ? 0 : entries.getIndex();
                                List<ServiceInstance> next = mapInstances(entries);
                                List<ServiceInstance> previous = instances.get();
                                if (!previous.equals(next)) {
                                    instances.set(next);
                                    publisher.submit(
                                            new ServiceInstancesChanged(
                                                    query, previous, next));
                                }
                                long nextIndex = ConsulIndex.next(index, returnedIndex);
                                schedule(
                                        Duration.ZERO,
                                        () -> poll(nextIndex, RETRY_INITIAL));
                            });
        }

        private void close() {
            running.set(false);
            publisher.close();
        }
    }
}
