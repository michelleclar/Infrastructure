package org.carl.infrastructure.redis.factory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisSentinelClientTest {

    private RedisClient redisClient;

    @BeforeAll
    public void setup() {
        RedisConfigOptions options =
                new RedisConfigOptions()
                        .setSentinelMasterName("mymaster")
                        .setConnectType(SentinelType.SENTINEL)
                        .setSentinelRole(SentinelRole.MASTER)
                        .addConnectionString("redis://172.16.250.140:26379")
                        .setPassword("qWm5k74mmx1o0enE");

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
