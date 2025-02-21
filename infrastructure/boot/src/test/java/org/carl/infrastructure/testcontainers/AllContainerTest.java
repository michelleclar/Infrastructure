package org.carl.infrastructure.testcontainers;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.carl.infrastructure.config.IProfile;
import org.carl.infrastructure.util.Utils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.ComposeContainer;

// @Testcontainers
public interface AllContainerTest {
    ComposeContainer compose =
            new ComposeContainer(new File("src/test/resources/composev2/compose-test.yml"));

    default void register() {
        for (ContainerType value : ContainerType.values()) {
            if (value.containerConfig != null) {
                withExposedService(value.containerConfig);
            }
        }
    }

    default void withExposedService(ContainerConfig containerConfig) {
        containerConfig.ports.forEach(
                port -> {
                    compose.withExposedService(containerConfig.containerName, port.servicePort());
                });
    }
}

class ContainerConfig {
    public final String containerName;
    public final List<PairPort> ports;
    public final List<String> commands;
    public final List<PairEnv> env;

    ContainerConfig(String containerName, List<PairPort> ports) {
        this.containerName = containerName;
        this.ports = ports;
        this.commands = null;
        this.env = null;
    }

    ContainerConfig(String containerName, List<PairPort> ports, List<String> commands) {
        this.containerName = containerName;
        this.ports = ports;
        this.commands = commands;
        this.env = null;
    }

    ContainerConfig(List<PairEnv> env, String containerName, List<PairPort> ports) {
        this.containerName = containerName;
        this.ports = ports;
        this.commands = null;
        this.env = env;
    }

    ContainerConfig(
            String containerName, List<PairPort> ports, List<String> commands, List<PairEnv> env) {
        this.containerName = containerName;
        this.ports = ports;
        this.commands = commands;
        this.env = env;
    }

    static PairEnv createEnv(String key, String value) {
        return new PairEnv(key, value);
    }

    static PairPort createPort(Integer innerPort, Integer servicePort) {
        return new PairPort(innerPort, servicePort);
    }

    record PairPort(Integer innerPort, Integer servicePort) {}

    record PairEnv(String key, String value) {}
}

enum ContainerType implements IProfile {
    REDIS,
    CONSUL,
    PG,
    PULSAR,
    ES,
    ETCD,
    MINIO,
    MILVUS;
    ContainerConfig containerConfig;

    ContainerType() {
        switch (this) {
            case REDIS -> this.containerConfig = createRedis();
            case ES -> this.containerConfig = createEs();
            case CONSUL -> this.containerConfig = createConsul();
            case PG -> this.containerConfig = createPg();
            case PULSAR -> this.containerConfig = createPulsar();
        }
    }

    private ContainerConfig createPulsar() {
        Optional<String> optional =
                ConfigProvider.getConfig()
                        .getOptionalValue("mp.messaging.incoming.data.serviceUrl", String.class);

        if (optional.isPresent()) {
            org.carl.infrastructure.util.UrlParser url;
            try {
                url = Utils.createURL(optional.get());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return new ContainerConfig(
                    getContainerName(), List.of(ContainerConfig.createPort(6650, url.getPort())));
        }
        return new ContainerConfig(
                getContainerName(), List.of(ContainerConfig.createPort(6650, 6650)));
    }

    private ContainerConfig createPg() {
        Optional<String> jdbcUrl =
                ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.datasource.jdbc.url", String.class);
        Optional<String> userName =
                ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.datasource.username", String.class);
        Optional<String> password =
                ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.datasource.password", String.class);
        if (jdbcUrl.isPresent() && userName.isPresent() && password.isPresent()) {

            return new ContainerConfig(
                    List.of(
                            ContainerConfig.createEnv("POSTGRES_USER", userName.get()),
                            ContainerConfig.createEnv("POSTGRES_PASSWORD", password.get()),
                            ContainerConfig.createEnv("POSTGRES_DB", "db")),
                    getContainerName(),
                    List.of(ContainerConfig.createPort(5432, 15432)));
        }

        return new ContainerConfig(
                List.of(
                        ContainerConfig.createEnv("POSTGRES_USER", "root"),
                        ContainerConfig.createEnv("POSTGRES_PASSWORD", "root"),
                        ContainerConfig.createEnv("POSTGRES_DB", "db")),
                getContainerName(),
                List.of(new ContainerConfig.PairPort(5432, 5432)));
    }

    private ContainerConfig createConsul() {
        InetSocketAddress localhost = new InetSocketAddress("localhost", 8500);
        Optional<String> optionalValue =
                ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.consul-config.agent.host-port", String.class);
        if (optionalValue.isPresent()) {
            String[] split = optionalValue.get().split(":");
            localhost = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        }
        return new ContainerConfig(
                getContainerName(),
                List.of(new ContainerConfig.PairPort(8500, localhost.getPort())),
                null);
    }

    private ContainerConfig createEs() {
        Optional<String> optional =
                ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.elasticsearch.hosts", String.class);
        InetSocketAddress localhost = new InetSocketAddress("localhost", 9200);
        if (optional.isPresent()) {
            String[] split = optional.get().split(":");
            localhost = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        }

        return new ContainerConfig(
                getContainerName(),
                List.of(new ContainerConfig.PairPort(9200, localhost.getPort())),
                null);
    }

    public ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    private ContainerConfig createRedis() {
        Optional<String> optionalValue =
                ConfigProvider.getConfig().getOptionalValue("quarkus.redis.hosts", String.class);
        if (optionalValue.isPresent()) {
            org.carl.infrastructure.util.UrlParser url;
            try {
                url = Utils.createURL(optionalValue.get());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return new ContainerConfig(
                    getContainerName(), List.of(new ContainerConfig.PairPort(6379, url.getPort())));
        }
        return new ContainerConfig(
                getContainerName(), List.of(new ContainerConfig.PairPort(6379, 6379)));
    }

    private String getContainerName() {
        return this.name().toLowerCase()
                + (isProdMode() ? "-prod" : isDevMode() ? "-dev" : "-test");
    }
}
