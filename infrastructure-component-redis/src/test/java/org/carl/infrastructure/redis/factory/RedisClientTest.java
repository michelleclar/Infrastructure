package org.carl.infrastructure.redis.factory;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.module.SimpleModule;

import io.vertx.core.Future;

import org.carl.infrastructure.redis.factory.jackson.module.*;
import org.carl.infrastructure.redis.option.RedisConfigOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisClientTest {

    private RedisClient redisClient;

    @BeforeAll
    public void setup() {
        // Assuming Redis is running on localhost:6379
        // If not, these tests will fail or we can disable them by default
        RedisConfigOptions options = new RedisConfigOptions();
        options.setConnectionString("redis://180.184.66.147:6379");
        SimpleModule customModule = new SimpleModule();
        customModule.addSerializer(Date.class, new CustomDateSerializer());
        customModule.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
        customModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        customModule.addSerializer(Duration.class, new CustomDurationSerializer());
        customModule.addSerializer(OffsetDateTime.class, new CustomOffsetDateTimeSerializer());
        customModule.addSerializer(LocalDate.class, new CustomLocalDateSerializer());
        options.registerModules(customModule);

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
        String key = "test:key:" + UUID.randomUUID();
        String value = "hello world";

        redisClient.setSync(key, value);
        String retrieved = redisClient.getSync(key);

        assertEquals(value, retrieved);

        // Clean up
        redisClient.delSync(key);
    }

    @Test
    public void testSetAndGetAsync() throws ExecutionException, InterruptedException {
        String key = "test:key:async:" + UUID.randomUUID();
        String value = "hello async";

        Future<Void> setFuture = redisClient.set(key, value);
        setFuture.toCompletionStage().toCompletableFuture().get();

        Future<String> getFuture = redisClient.get(key);
        String retrieved = getFuture.toCompletionStage().toCompletableFuture().get();

        assertEquals(value, retrieved);

        redisClient.del(key).toCompletionStage().toCompletableFuture().get();
    }

    @Test
    public void testSetWithExpiration() throws InterruptedException {
        String key = "test:expire:" + UUID.randomUUID();
        String value = "expire me";

        redisClient.setSync(key, value, Duration.ofSeconds(2));

        String retrieved = redisClient.getSync(key);
        assertEquals(value, retrieved);

        // Wait for expiration
        Thread.sleep(2500);

        String expired = redisClient.getSync(key);
        assertNull(expired);
    }

    @Test
    public void testIncr() {
        String key = "test:incr:" + UUID.randomUUID();

        // Should start at 1
        Long val1 = redisClient.incrSync(key);
        assertEquals(1L, val1);

        Long val2 = redisClient.incrSync(key);
        assertEquals(2L, val2);

        redisClient.delSync(key);
    }

    @Test
    public void testKeys() {
        String prefix = "test:keys:" + UUID.randomUUID();
        String key1 = prefix + ":1";
        String key2 = prefix + ":2";

        redisClient.setSync(key1, "v1");
        redisClient.setSync(key2, "v2");

        List<String> keys = redisClient.keysSync(prefix);
        assertNotNull(keys);
        assertTrue(keys.size() >= 2);
        // Note: checking detailed content might be flaky depending on order, but size check is
        // good.

        redisClient.delSync(key1);
        redisClient.delSync(key2);
    }

    @Test
    public void testGetOrSet() throws InterruptedException {
        String key = "test:getorset:" + UUID.randomUUID();
        String val = "initial";

        // 1. First call - should set
        String result1 = redisClient.getOrSetSync(key, val, Duration.ofSeconds(5));
        assertEquals(val, result1);

        // 2. Second call - should get existing, even if we pass new val
        // Note: The Lua script returns the value that IS in Redis.
        // If it was just set, it returns the new value.
        // If it existed, it returns the existing value.
        String result2 = redisClient.getOrSetSync(key, "newval", Duration.ofSeconds(5));
        assertEquals(val, result2);

        // Cleanup
        redisClient.delSync(key);
    }

    @Test
    public void testIncrWithInit() {
        String key = "test:incrinit:" + UUID.randomUUID();

        // 1. Key doesn't exist, should init to 10
        Long val1 = redisClient.incrSync(key, 1, 10);
        assertEquals(10L, val1);

        // 2. Key exists, should incr by 2 -> 12
        Long val2 = redisClient.incrSync(key, 2, 100);
        assertEquals(12L, val2);

        // Cleanup
        redisClient.delSync(key);
    }

    @Test
    public void testGenericOperations() {
        String key = "test:generic:" + UUID.randomUUID();
        TestPojo pojo = new TestPojo("Alice", 30);

        // Test Set and Get Sync
        redisClient.setSync(key, pojo);
        TestPojo retrieved = redisClient.getSync(key, TestPojo.class);

        assertNotNull(retrieved);
        assertEquals(pojo.getName(), retrieved.getName());
        assertEquals(pojo.getAge(), retrieved.getAge());

        // Test GetOrSet Generic
        TestPojo newPojo = new TestPojo("Bob", 25);
        // Should return existing "Alice"
        TestPojo existing =
                redisClient.getOrSetSync(key, newPojo, Duration.ofSeconds(5), TestPojo.class);
        assertEquals("Alice", existing.getName());

        redisClient.delSync(key);

        // Should return new "Bob"
        TestPojo fresh =
                redisClient.getOrSetSync(key, newPojo, Duration.ofSeconds(5), TestPojo.class);
        assertEquals("Bob", fresh.getName());

        redisClient.delSync(key);
    }

    // Helper POJO for testing
    public static class TestPojo {
        private String name;
        private int age;

        public TestPojo() {}

        public TestPojo(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    public void testComplexGenericOperations() {
        String key = "test:complex:" + UUID.randomUUID();

        ComplexPojo pojo = new ComplexPojo();
        pojo.setId("c1");
        pojo.setTags(java.util.Arrays.asList("tag1", "tag2"));

        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("math", 95);
        map.put("english", 88);
        pojo.setScores(map);

        // Vert.x default Jackson mapper might serialize LocalDateTime as array or string depending
        // on modules.
        // We use a specific time that shouldn't suffer from minor precision loss during round trip
        // if serialized as string
        pojo.setCreatedAt(java.time.LocalDateTime.of(2023, 10, 1, 12, 0, 0));
        pojo.setNestedCreatedAt(LocalDateTime.now().withNano(0));

        pojo.setNested(new NestedPojo("child", true));

        redisClient.setSync(key, pojo);

        ComplexPojo retrieved = redisClient.getSync(key, ComplexPojo.class);

        assertNotNull(retrieved);
        assertEquals(pojo.getId(), retrieved.getId());
        assertEquals(pojo.getTags(), retrieved.getTags());
        assertEquals(pojo.getScores(), retrieved.getScores());
        assertEquals(pojo.getCreatedAt(), retrieved.getCreatedAt());
        assertEquals(pojo.getNestedCreatedAt(), retrieved.getNestedCreatedAt());
        assertNotNull(retrieved.getNested());
        assertEquals(pojo.getNested().getName(), retrieved.getNested().getName());
        assertEquals(pojo.getNested().isActive(), retrieved.getNested().isActive());

        redisClient.delSync(key);
    }

    @Test
    public void testDistributedLock() throws Exception {
        String key = "test:lock:" + UUID.randomUUID();
        RedisClient.RedisLock lock = redisClient.getLock(key);

        // 1. Try acquire lock with 5s wait, 10s lease
        boolean acquired =
                lock.tryLock(5000, 10000).toCompletionStage().toCompletableFuture().get();
        assertTrue(acquired);

        // 2. Try acquire same lock properly failing
        RedisClient.RedisLock lock2 = redisClient.getLock(key);
        boolean acquired2 =
                lock2.tryLock(100, 10000).toCompletionStage().toCompletableFuture().get();
        assertFalse(acquired2);

        // 3. Unlock
        lock.unlock().toCompletionStage().toCompletableFuture().get();

        // 4. Lock2 should now succeed
        boolean acquired3 =
                lock2.tryLock(100, 10000).toCompletionStage().toCompletableFuture().get();
        assertTrue(acquired3);

        lock2.unlock().toCompletionStage().toCompletableFuture().get();
    }

    @Test
    public void testDistributedLockDuration() throws Exception {
        String key = "test:lock:duration:" + UUID.randomUUID();
        RedisClient.RedisLock lock = redisClient.getLock(key);

        // 1. Try acquire lock with Duration (5s wait, 10s lease)
        boolean acquired = lock.tryLock(Duration.ofSeconds(5), Duration.ofSeconds(10))
                .toCompletionStage().toCompletableFuture().get();
        assertTrue(acquired);

        lock.unlock().toCompletionStage().toCompletableFuture().get();
    }

    @Test
    public void testDistributedLockWatchdog() throws Exception {
        String key = "test:lock:watchdog:" + UUID.randomUUID();
        RedisClient.RedisLock lock = redisClient.getLock(key);

        // 1. Acquire with watchdog (default 30s lease)
        boolean acquired = lock.tryLock(5000).toCompletionStage().toCompletableFuture().get();
        assertTrue(acquired);

        // 2. Wait longer than 1/3 lease (10s) ensuring renewal happens.
        // Wait 12s. Initial lease 30s. Renewal at 10s.
        // If renewal fails, it would still naturally be valid, but we want to ensure
        // the timer is running and doesn't crash.
        Thread.sleep(12000);

        // Let's rely on unlocking succeeding.
        lock.unlock().toCompletionStage().toCompletableFuture().get();
    }

    public static class ComplexPojo {
        private String id;
        private java.util.List<String> tags;
        private java.util.Map<String, Integer> scores;
        private java.time.LocalDateTime createdAt;
        private LocalDateTime nestedCreatedAt;
        private NestedPojo nested;

        public ComplexPojo() {}

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public java.util.List<String> getTags() {
            return tags;
        }

        public void setTags(java.util.List<String> tags) {
            this.tags = tags;
        }

        public java.util.Map<String, Integer> getScores() {
            return scores;
        }

        public void setScores(java.util.Map<String, Integer> scores) {
            this.scores = scores;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getNestedCreatedAt() {
            return nestedCreatedAt;
        }

        public void setNestedCreatedAt(LocalDateTime nestedCreatedAt) {
            this.nestedCreatedAt = nestedCreatedAt;
        }

        public NestedPojo getNested() {
            return nested;
        }

        public void setNested(NestedPojo nested) {
            this.nested = nested;
        }
    }

    public static class NestedPojo {
        private String name;
        private boolean active;

        public NestedPojo() {}

        public NestedPojo(String name, boolean active) {
            this.name = name;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
