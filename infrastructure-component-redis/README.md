# infrastructure-component-redis

> 基于 Vert.x Redis Client 的轻量封装。提供同步/异步 KV 操作、泛型对象序列化、原子 Lua 脚本操作，以及内置 Watchdog 自动续期的分布式锁。支持 Standalone、Sentinel、Cluster、Replication 四种连接模式。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-redis"))
}
```

运行时要求：JDK 21，可访问的 Redis 实例。

---

## 核心类

| 类 / 枚举 | 用途 |
|---|---|
| `RedisClientFactory` | 工厂，创建 `RedisClient` 实例 |
| `RedisClient` | 主操作门面：KV、TTL、批量删、原子操作、分布式锁 |
| `RedisClient.RedisLock` | 分布式锁句柄，通过 `RedisClient.getLock(key)` 获取 |
| `RedisConfigOptions` | 连接参数构建器（链式 API） |
| `SentinelType` | 枚举：`STANDALONE` / `SENTINEL` / `CLUSTER` / `REPLICATION` |
| `SentinelRole` | 枚举：`MASTER` / `REPLICA` / `SENTINEL` |

---

## 配置

本模块**不使用** `@ConfigMapping` / MicroProfile Config 注解，连接参数通过代码构建 `RedisConfigOptions` 传入。

| `RedisConfigOptions` 方法 | 说明 |
|---|---|
| `setConnectionString(String)` | 单节点连接串，格式 `redis://[user:password@]host:port[/db]` |
| `addConnectionString(String)` | 追加一个节点地址（Sentinel / Cluster 多节点） |
| `setConnectType(SentinelType)` | 连接模式，默认 `STANDALONE` |
| `setSentinelRole(SentinelRole)` | Sentinel 模式下的角色 |
| `setSentinelMasterName(String)` | Sentinel 主节点名称 |
| `setPassword(String)` | Redis 认证密码 |
| `setMaxPoolSize(int)` | 连接池最大连接数 |
| `setMaxPoolWaiting(int)` | 连接池最大等待队列长度 |
| `setConnectTimeout(int)` | 连接超时（毫秒） |
| `registerModules(Module)` | 向 Vert.x 全局 Jackson mapper 注册自定义序列化模块 |

---

## RedisClient API

所有方法均有异步（返回 `CompletableFuture`）和同步（`xxxSync`，内部调用 `.join()`）两套重载。

### 基础 KV

| 方法签名 | 说明 |
|---|---|
| `CompletableFuture<String> get(String key)` | 获取字符串值 |
| `<T> CompletableFuture<T> get(String key, Function<String, T> function)` | 获取并转换 |
| `<T> CompletableFuture<T> get(String key, Class<T> clazz)` | 获取并反序列化为对象 |
| `<T> CompletableFuture<T> get(String key, TypeReference<T> typeRef)` | 获取并反序列化（泛型容器，如 `List<Foo>`） |
| `CompletableFuture<Response> set(String key, String value)` | 设置字符串值 |
| `CompletableFuture<Response> set(String key, String value, Duration duration)` | 设置字符串值并指定过期时间 |
| `<T> CompletableFuture<Response> set(String key, T value)` | 序列化对象后写入 |
| `<T> CompletableFuture<Response> set(String key, T value, Duration duration)` | 序列化对象后写入并指定过期时间 |
| `CompletableFuture<Response> del(String key)` | 删除单个 key |
| `CompletableFuture<Response> del(List<String> keys)` | 批量删除 |
| `CompletableFuture<Long> pttl(String key)` | 获取 key 剩余毫秒 TTL（-2 不存在，-1 无过期） |
| `CompletableFuture<List<String>> keys(String prefix)` | 按前缀查找 key（内部追加 `*`） |

### 原子操作（Lua 脚本）

| 方法签名 | 说明 |
|---|---|
| `CompletableFuture<Long> incr(String key)` | 自增（key 不存在时从 0+1 开始） |
| `CompletableFuture<Long> incr(String key, long by, long init)` | key 存在时加 `by`，不存在时初始化为 `init` |
| `CompletableFuture<Long> incr(String key, long init)` | key 存在时加 1，不存在时初始化为 `init` |
| `CompletableFuture<String> getOrSet(String key, String value, Duration duration)` | 原子 Get-or-Set：key 存在返回现有值，不存在则写入并返回 `value` |
| `<T> CompletableFuture<T> getOrSet(String key, T value, Duration duration, Class<T> clazz)` | 泛型版 Get-or-Set |

### 分布式锁

```java
RedisClient.RedisLock lock = redisClient.getLock("lock:order:12345");
```

| 方法签名 | 说明 |
|---|---|
| `CompletableFuture<Boolean> tryLock(long waitTime, long leaseTime)` | 尝试加锁，`waitTime` / `leaseTime` 单位毫秒，无自动续期 |
| `CompletableFuture<Boolean> tryLock(Duration waitTime, Duration leaseTime)` | 同上，`Duration` 重载 |
| `CompletableFuture<Boolean> tryLock(long waitTime)` | 带 Watchdog 自动续期（初始 lease 30s，每 10s 续期），`waitTime` 毫秒 |
| `CompletableFuture<Boolean> tryLock(Duration waitTime)` | 同上，`Duration` 重载 |
| `CompletableFuture<Response> unlock()` | 释放锁并停止 Watchdog |

锁实现：`SET key value NX PX leaseTime`；解锁使用 Lua 脚本保证原子性（只删自己持有的锁）；续期同样通过 Lua 脚本 `PEXPIRE`。

---

## 使用示例

### 1. Standalone 连接（最小配置）

```java
// 无参数：连接 localhost:6379
try (RedisClient client = RedisClientFactory.create()) {
    client.setSync("greeting", "hello");
    String value = client.getSync("greeting");
    System.out.println(value); // hello
}
```

### 2. 自定义连接参数

```java
RedisConfigOptions options = new RedisConfigOptions()
        .setConnectionString("redis://192.168.1.100:6379")
        .setPassword("mypassword")
        .setMaxPoolSize(16)
        .setMaxPoolWaiting(64);

try (RedisClient client = RedisClientFactory.create(options)) {
    // 带过期时间写入
    client.setSync("session:abc", "token-value", Duration.ofMinutes(30));

    // 泛型对象读写
    client.setSync("user:1", new UserProfile("alice", 28));
    UserProfile user = client.getSync("user:1", UserProfile.class);

    // 泛型容器（List / Map 等）
    client.setSync("tags:1", List.of("java", "redis"));
    List<String> tags = client.getSync("tags:1", new TypeReference<List<String>>() {});

    // 原子 Get-or-Set：缓存穿透防护
    UserProfile cached = client.getOrSetSync(
            "user:1", new UserProfile("default", 0), Duration.ofMinutes(5), UserProfile.class);

    // 原子计数器（不存在时初始化为 1000，之后每次 +1）
    Long count = client.incrSync("counter:pv", 1, 1000);

    // 按前缀批量删除
    List<String> keys = client.keysSync("session:");
    client.delSync(keys);
}
```

### 3. Sentinel 模式

```java
RedisConfigOptions options = new RedisConfigOptions()
        .setConnectType(SentinelType.SENTINEL)
        .setSentinelRole(SentinelRole.MASTER)
        .setSentinelMasterName("mymaster")
        .addConnectionString("redis://sentinel1:26379")
        .addConnectionString("redis://sentinel2:26379")
        .addConnectionString("redis://sentinel3:26379")
        .setPassword("mypassword");

try (RedisClient client = RedisClientFactory.create(options)) {
    client.setSync("key", "value");
}
```

### 4. 分布式锁

```java
RedisClient.RedisLock lock = client.getLock("lock:order:12345");

// 固定 lease 时间（适合已知耗时短的操作）
boolean acquired = lock.tryLock(Duration.ofSeconds(5), Duration.ofSeconds(10)).join();
if (acquired) {
    try {
        // 业务逻辑
    } finally {
        lock.unlock().join();
    }
}

// Watchdog 自动续期（适合耗时不确定的操作）
boolean acquired = lock.tryLock(Duration.ofSeconds(5)).join(); // lease 30s，每 10s 续期
if (acquired) {
    try {
        // 长时间业务逻辑
    } finally {
        lock.unlock().join(); // unlock 同时取消 Watchdog 定时器
    }
}
```

### 5. 注册自定义 Jackson 序列化模块

```java
SimpleModule module = new SimpleModule();
module.addSerializer(LocalDateTime.class, new MyLocalDateTimeSerializer());
module.addDeserializer(LocalDateTime.class, new MyLocalDateTimeDeserializer());

RedisConfigOptions options = new RedisConfigOptions()
        .setConnectionString("redis://localhost:6379");
options.registerModules(module);  // 注册到 Vert.x 全局 Jackson mapper

RedisClient client = RedisClientFactory.create(options);
```

> **注意**：`registerModules` 修改 Vert.x 进程级全局 `DatabindCodec.mapper()`，对同一 JVM 内所有 Vert.x JSON 操作生效，只需注册一次。

---

## 注意事项

1. **`RedisClient` 是 `AutoCloseable`**，建议在 try-with-resources 或应用关闭钩子中调用 `close()`，释放底层 Vert.x 连接和线程。
2. **`keysSync` / `keys` 使用 Redis `KEYS` 命令**（内部追加 `*`），生产环境大数据量时有阻塞风险，建议仅在数据量可控或非关键路径使用。
3. **同步方法调用 `.join()`**，若在 Vert.x EventLoop 线程上调用会阻塞事件循环；建议在 Worker 线程或非 EventLoop 上下文中使用 Sync 系列方法，异步场景优先使用 `CompletableFuture` 系列。
4. **分布式锁的 Watchdog** 依赖 Vert.x 定时器，`unlock()` 会自动取消定时器，确保每次业务结束都调用 `unlock()`，避免定时器泄漏。
5. **`RedisClientFactory.create()` 会自动注册 `JavaTimeModule`**（`LocalDateTime` 等序列化为 ISO 字符串而非时间戳数组），如需自定义时间格式，在 `create` 之后通过 `options.registerModules(...)` 覆盖即可。

---

## 测试

测试通过环境变量激活，未设置时跳过：

```bash
# Standalone 测试
TEST_REDIS_URL=redis://localhost:6379 \
./gradlew :infrastructure-component-redis:test

# Sentinel 测试
TEST_REDIS_SENTINEL_URL=redis://sentinel1:26379 \
TEST_REDIS_SENTINEL_MASTER=mymaster \
./gradlew :infrastructure-component-redis:test
```

---

## License

跟随项目主 LICENSE。
