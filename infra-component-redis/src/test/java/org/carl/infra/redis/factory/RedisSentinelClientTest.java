package org.carl.infra.redis.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisSentinelClientTest {

    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:7.4.9-alpine");
    private static final String MASTER_NAME = "testmaster";

    private GenericContainer<?> sentinel;
    private RedisClient redisClient;

    @BeforeAll
    public void setup() throws Exception {
        int[] ports = findTwoFreePorts();
        int masterPort = ports[0];
        int sentinelPort = ports[1];
        String config =
                "port "
                        + sentinelPort
                        + "\n"
                        + "sentinel monitor "
                        + MASTER_NAME
                        + " 127.0.0.1 "
                        + masterPort
                        + " 1\n"
                        + "sentinel down-after-milliseconds "
                        + MASTER_NAME
                        + " 5000\n"
                        + "sentinel failover-timeout "
                        + MASTER_NAME
                        + " 10000\n";

        sentinel =
                new GenericContainer<>(REDIS_IMAGE)
                        .withExposedPorts(masterPort, sentinelPort)
                        .withCopyToContainer(
                                Transferable.of(
                                        config.getBytes(StandardCharsets.UTF_8), 0777),
                                "/tmp/sentinel.conf")
                        .withCommand(
                                "sh",
                                "-c",
                                "redis-server --port "
                                        + masterPort
                                        + " --save '' --appendonly no"
                                        + " & exec redis-server /tmp/sentinel.conf --sentinel")
                        .waitingFor(Wait.forListeningPort());
        sentinel.setPortBindings(
                List.of(
                        masterPort + ":" + masterPort,
                        sentinelPort + ":" + sentinelPort));
        sentinel.start();

        RedisConfigOptions options =
                new RedisConfigOptions()
                        .setSentinelMasterName(MASTER_NAME)
                        .setConnectType(SentinelType.SENTINEL)
                        .setSentinelRole(SentinelRole.MASTER)
                        .setSentinelAutoFailover(true)
                        .addConnectionString(
                                "redis://127.0.0.1:" + sentinelPort);
        redisClient = RedisClientFactory.create(options);
    }

    @AfterAll
    public void tearDown() throws Exception {
        if (redisClient != null) {
            redisClient.close();
        }
        if (sentinel != null) {
            sentinel.stop();
        }
    }

    @Test
    public void testSetAndGetSync() {
        String key = "test:sentinel:" + UUID.randomUUID();
        String value = "hello sentinel";

        redisClient.setSync(key, value);
        String retrieved = redisClient.getSync(key);

        assertNotNull(retrieved);
        assertEquals(value, retrieved);
        redisClient.delSync(key);
    }

    private static int[] findTwoFreePorts() throws Exception {
        try (ServerSocket first = new ServerSocket(0);
                ServerSocket second = new ServerSocket(0)) {
            return new int[] {first.getLocalPort(), second.getLocalPort()};
        }
    }
}
