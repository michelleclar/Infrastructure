# infra-component-redis

> 基于 Vert.x Redis Client 的独立轻量封装。提供带命令超时的同步/异步 KV 操作、泛型对象序列化、分页 SCAN、原子 Lua 脚本操作，以及带生命周期通知的 Watchdog 分布式锁。支持 Standalone、Sentinel、Cluster、Replication 四种连接模式。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infra-component-redis"))
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
| `setMaxWaitingHandlers(int)` | 单连接 pipeline 最大等待响应数量 |
| `setConnectTimeout(int)` | 连接超时（毫秒） |
| `setCommandTimeout(Duration)` | 单条命令总超时，默认 30 秒 |
| `setSentinelAutoFailover(boolean)` | Sentinel master 连接自动故障切换 |
| `setUseReplicas(RedisReplicas)` | Cluster / Replication 读取副本策略 |
| `setClusterTransactions(RedisClusterTransactions)` | Cluster 事务策略 |
| `setNetClientOptions(NetClientOptions)` | TLS、TCP、idle timeout 等底层连接配置 |
| `setTracingPolicy(TracingPolicy)` | Vert.x tracing 策略 |
| `setPoolName(String)` / `setMetricsName(String)` | 连接池与客户端指标名称 |
| `registerModules(Module)` | 为当前 Redis 客户端配置自定义 Jackson 序列化模块 |

`setDatabase(int)` 不再静默忽略调用，而是直接抛出 `UnsupportedOperationException`。数据库必须写在连接 URI 中，例如 `redis://localhost:6379/2`；Redis Cluster 不支持多数据库。

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
| `CompletableFuture<ScanPage> scan(String prefix, String cursor)` | 分页扫描字面前缀，首次 cursor 传 `"0"` |
| `CompletableFuture<ScanPage> scan(String prefix, String cursor, int count)` | 带 Redis `COUNT` hint 的分页扫描 |
| `CompletableFuture<List<String>> keys(String prefix)` | 使用 SCAN 汇总全部匹配 key，结果量大时应改用分页 API |

Cluster 模式下，连接无状态的 Vert.x `SCAN` 只访问一个节点，因此本模块会明确拒绝 `scan` / `keys`，避免返回不完整结果。

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
| `CompletableFuture<LockTermination> terminationFuture()` | 锁正常释放时返回 `RELEASED`，租约过期或续期失败时返回 `LOST` |
| `boolean isHeldByThisInstance()` | 当前句柄是否仍处于本地持锁状态 |

锁实现：每次加锁生成新的 UUID token，通过 `SET key value NX PX leaseTime` 获取；解锁使用 Lua 脚本保证只删除当前 token；续期通过 Lua 脚本 `PEXPIRE`。同一个锁句柄不允许并发或重复获取，续期请求不会重叠。

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

工厂创建的客户端拥有其内部 Vert.x，`close()` 会关闭 Redis 和 Vert.x。应用已有共享 Vert.x 时，应显式注入；客户端关闭时不会关闭外部 Vert.x：

```java
Vertx vertx = Vertx.vertx();
RedisClient client = RedisClientFactory.create(vertx, options);
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
    lock.terminationFuture().thenAccept(termination -> {
        if (termination == RedisClient.RedisLock.LockTermination.LOST) {
            // 停止后续业务写入，进入补偿或重试流程
        }
    });
    try {
        // 长时间业务逻辑
    } finally {
        lock.unlock().join(); // unlock 同时取消 Watchdog 定时器
    }
}
```

该锁基于单 Redis 实例的租约语义，不提供 fencing token。资金、库存、订单状态迁移等需要强一致互斥的场景，受保护资源必须额外校验 fencing token 或使用具备该能力的协调系统。

### 5. 注册自定义 Jackson 序列化模块

```java
SimpleModule module = new SimpleModule();
module.addSerializer(LocalDateTime.class, new MyLocalDateTimeSerializer());
module.addDeserializer(LocalDateTime.class, new MyLocalDateTimeDeserializer());

RedisConfigOptions options = new RedisConfigOptions()
        .setConnectionString("redis://localhost:6379");
options.registerModules(module);  // 仅对使用该 options 创建的 RedisClient 生效

RedisClient client = RedisClientFactory.create(options);
```

> **注意**：`registerModules` 不会修改 Vert.x 进程级全局 `DatabindCodec.mapper()`。每组自定义模块由对应的 `RedisClient` 独立持有。

---

## 注意事项

1. **`RedisClient` 是 `AutoCloseable`**。工厂内部创建的 Vert.x 会随客户端关闭；注入的共享 Vert.x 由调用方管理。事件循环内需要等待关闭完成时使用 `closeAsync()`，不要阻塞事件循环。
2. **`keysSync` / `keys` 会用 SCAN 汇总全部结果**，不会执行阻塞式 `KEYS`，但仍可能占用大量内存；大结果集使用分页 `scan`。
3. **同步方法调用 `.join()`**，若在 Vert.x EventLoop 线程上调用会阻塞事件循环；建议在 Worker 线程或非 EventLoop 上下文中使用 Sync 系列方法，异步场景优先使用 `CompletableFuture` 系列。
4. **所有命令默认 30 秒超时**，通过 `setCommandTimeout(Duration)` 调整；连接超时由 `setConnectTimeout(int)` 单独控制。
5. **分布式锁的 Watchdog** 在续期失败时停止并将 `terminationFuture()` 完成为 `LOST`。业务必须处理锁丢失，且每次成功获取后都应在 `finally` 中调用 `unlock()`。
6. **`RedisClientFactory` 的默认 Codec 在类加载时初始化一次**，内置 `JavaTimeModule`（`LocalDateTime` 等序列化为 ISO 字符串而非时间戳数组）。如需自定义时间格式，在调用 `create(options)` 前通过 `options.registerModules(...)` 配置。

---

## 测试

Standalone、Sentinel、Cluster、Replication 测试使用 Testcontainers 和官方 `redis:7.4.9-alpine` 镜像；Docker 不可用时仅跳过容器测试，生命周期、超时和 Codec 单元测试仍会执行：

```bash
./gradlew :infra-component-redis:test
```

---

## License

跟随项目主 LICENSE。
