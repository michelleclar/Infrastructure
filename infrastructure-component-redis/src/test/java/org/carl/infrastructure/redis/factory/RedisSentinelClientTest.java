package org.carl.infrastructure.redis.factory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named = "TEST_REDIS_SENTINEL_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisSentinelClientTest {

    private RedisClient redisClient;

    @BeforeAll
    public void setup() {
        String sentinelUrl = System.getenv("TEST_REDIS_SENTINEL_URL");
        String masterName = System.getenv().getOrDefault("TEST_REDIS_SENTINEL_MASTER", "mymaster");
        String password = System.getenv("TEST_REDIS_SENTINEL_PASSWORD");
        RedisConfigOptions options =
                new RedisConfigOptions()
                        .setSentinelMasterName(masterName)
                        .setConnectType(SentinelType.SENTINEL)
                        .setSentinelRole(SentinelRole.MASTER)
                        .addConnectionString(sentinelUrl);
        if (password != null && !password.isEmpty()) {
            options.setPassword(password);
        }

        redisClient = RedisClientFactory.create(options);
    }

    @AfterAll
    public void tearDown() throws Exception {
        if (redisClient != null) {
            redisClient.close();
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
}
