package org.carl.infra.redis.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.redis.client.RedisReplicas;
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
import java.util.List;
import java.util.UUID;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisReplicationClientTest {

    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:7.4.9-alpine");

    private GenericContainer<?> replication;
    private RedisClient redisClient;

    @BeforeAll
    void setup() throws Exception {
        int[] ports = findTwoFreePorts();
        int primaryPort = ports[0];
        int replicaPort = ports[1];

        replication =
                new GenericContainer<>(REDIS_IMAGE)
                        .withExposedPorts(primaryPort, replicaPort)
                        .withCommand(
                                "sh",
                                "-c",
                                "redis-server --port "
                                        + primaryPort
                                        + " --save '' --appendonly no"
                                        + " & exec redis-server --port "
                                        + replicaPort
                                        + " --save '' --appendonly no"
                                        + " --replicaof 127.0.0.1 "
                                        + primaryPort)
                        .waitingFor(Wait.forListeningPort());
        replication.setPortBindings(
                List.of(
                        primaryPort + ":" + primaryPort,
                        replicaPort + ":" + replicaPort));
        replication.start();
        waitForReplicaLink(replicaPort);

        RedisConfigOptions options =
                new RedisConfigOptions()
                        .setConnectType(SentinelType.REPLICATION)
                        .setUseReplicas(RedisReplicas.ALWAYS)
                        .addConnectionString("redis://127.0.0.1:" + primaryPort);
        redisClient = RedisClientFactory.create(options);
    }

    @AfterAll
    void tearDown() {
        if (redisClient != null) {
            redisClient.close();
        }
        if (replication != null) {
            replication.stop();
        }
    }

    @Test
    void writesToPrimaryAndReadsFromReplica() throws Exception {
        String key = "test:replication:" + UUID.randomUUID();
        redisClient.setSync(key, "value");

        String value = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            value = redisClient.getSync(key);
            if ("value".equals(value)) {
                break;
            }
            Thread.sleep(100);
        }

        assertEquals("value", value);
        redisClient.delSync(key);
    }

    private void waitForReplicaLink(int replicaPort) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            Container.ExecResult result =
                    replication.execInContainer(
                            "redis-cli", "-p", Integer.toString(replicaPort), "info", "replication");
            if (result.getStdout().contains("master_link_status:up")) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Redis replica did not reach master_link_status:up");
    }

    private static int[] findTwoFreePorts() throws Exception {
        try (ServerSocket first = new ServerSocket(0);
                ServerSocket second = new ServerSocket(0)) {
            return new int[] {first.getLocalPort(), second.getLocalPort()};
        }
    }
}
