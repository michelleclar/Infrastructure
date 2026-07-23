package org.carl.infra.redis.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisClusterClientTest {

    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:7.4.9-alpine");

    private GenericContainer<?> clusterNode;
    private RedisClient redisClient;

    @BeforeAll
    void setup() throws Exception {
        int[] ports = findTwoFreePorts();
        int clientPort = ports[0];
        int clusterBusPort = ports[1];

        clusterNode =
                new GenericContainer<>(REDIS_IMAGE)
                        .withExposedPorts(6379, 16379)
                        .withCommand(
                                "redis-server",
                                "--port",
                                "6379",
                                "--cluster-enabled",
                                "yes",
                                "--cluster-config-file",
                                "/tmp/nodes.conf",
                                "--cluster-node-timeout",
                                "5000",
                                "--appendonly",
                                "no",
                                "--protected-mode",
                                "no",
                                "--cluster-announce-ip",
                                "127.0.0.1",
                                "--cluster-announce-port",
                                Integer.toString(clientPort),
                                "--cluster-announce-bus-port",
                                Integer.toString(clusterBusPort))
                        .waitingFor(Wait.forListeningPort());
        clusterNode.setPortBindings(
                List.of(clientPort + ":6379", clusterBusPort + ":16379"));
        clusterNode.start();

        Container.ExecResult addSlots =
                clusterNode.execInContainer(
                        "redis-cli",
                        "-p",
                        "6379",
                        "cluster",
                        "addslotsrange",
                        "0",
                        "16383");
        assertEquals(0, addSlots.getExitCode(), addSlots.getStderr());
        waitForClusterReady();

        RedisConfigOptions options =
                new RedisConfigOptions()
                        .setConnectType(SentinelType.CLUSTER)
                        .addConnectionString("redis://127.0.0.1:" + clientPort);
        redisClient = RedisClientFactory.create(options);
    }

    @AfterAll
    void tearDown() {
        if (redisClient != null) {
            redisClient.close();
        }
        if (clusterNode != null) {
            clusterNode.stop();
        }
    }

    @Test
    void supportsKeyValueLuaAndLockOperations() {
        String key = "test:cluster:{" + UUID.randomUUID() + "}:value";
        redisClient.setSync(key, "value", Duration.ofSeconds(5));
        assertEquals("value", redisClient.getSync(key));

        assertEquals(
                "value",
                redisClient.getOrSetSync(key, "other", Duration.ofSeconds(5)));

        RedisClient.RedisLock lock = redisClient.getLock(key + ":lock");
        assertTrue(lock.tryLock(Duration.ofSeconds(1), Duration.ofSeconds(5)).join());
        lock.unlock().join();
        redisClient.delSync(key);
    }

    private void waitForClusterReady() throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            Container.ExecResult result =
                    clusterNode.execInContainer(
                            "redis-cli", "-p", "6379", "cluster", "info");
            if (result.getStdout().contains("cluster_state:ok")) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Redis Cluster did not reach cluster_state:ok");
    }

    private static int[] findTwoFreePorts() throws Exception {
        try (ServerSocket first = new ServerSocket(0);
                ServerSocket second = new ServerSocket(0)) {
            return new int[] {first.getLocalPort(), second.getLocalPort()};
        }
    }
}
