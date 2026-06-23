# infrastructure-component-quarkus / cache

为 Quarkus CDI 应用提供双层缓存抽象：以 **Caffeine** 作本地内存缓存，以 **Redis**（通过
`quarkus-redis-client`）作远端缓存。上层通过 `CacheService` / `CacheStd` 统一访问，
所有读写操作均返回 Mutiny `Uni`，与 Quarkus 响应式编程模型无缝衔接。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:cache"))
}
```

底层依赖（来自 `libs.bundles.cache`）：

| 库 | 说明 |
|----|------|
| `io.quarkus:quarkus-cache` | Quarkus Cache 扩展，提供 `CacheManager` |
| `io.quarkus:quarkus-redis-client` | Quarkus Redis 扩展，提供 `ReactiveRedisDataSource` |
| `com.github.ben-manes.caffeine:caffeine` | 本地内存缓存实现（由 `quarkus-cache` 传递引入） |

---

## 构建开关

`CacheService` bean 受编译期属性控制，**默认不激活**，需在 `application.properties` 中显式开启：

```properties
quarkus.plugins.cache.enable=true
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `quarkus.plugins.cache.enable` | `String` | 设为 `"true"` 时才会将 `CacheService` 注册为 CDI bean |

> 这是 `@IfBuildProperty` 构建时开关，**更改后需重新编译**，运行期修改无效。

---

## 架构

```
CacheService  (@ApplicationScoped, 受构建开关控制)
    └── extends CacheStd  (implements ICacheProvider)
                    │
                    └── injects CacheContext  (由 CacheContextProvider @Produces)
                                    │
                                    ├── LocalCacheContext   (Caffeine，内存)
                                    └── RemoteCacheContext  (Redis，响应式)

ICacheOperations  (标记接口，当前无方法，供扩展用)
ICacheProvider    (接口：getCacheContext())
BaseCache         (接口：定义 get / set / del / keys)
```

### 类 / 接口一览

| 类 / 接口 | 作用 |
|-----------|------|
| `ICacheProvider` | 接口：声明 `getCacheContext()` 方法 |
| `ICacheOperations` | 标记接口，当前无方法体 |
| `CacheStd` | 实现 `ICacheProvider` 的基类，通过 `@Inject` 持有 `CacheContext` |
| `CacheService` | 继承 `CacheStd`、实现 `ICacheOperations` 的 `@ApplicationScoped` bean |
| `CacheContextProvider` | CDI `@Produces`，生产 `@ApplicationScoped` 的 `CacheContext` bean |
| `CacheContext` | 同时持有 `LocalCacheContext` 和 `RemoteCacheContext` 两层上下文 |
| `CacheContext.LocalCacheContext` | 基于 Caffeine 的本地缓存，实现 `BaseCache` |
| `CacheContext.RemoteCacheContext` | 基于 Redis 响应式客户端的远端缓存，实现 `BaseCache` |
| `BaseCache` | 接口：定义四个基础缓存操作 |
| `CacheType` | 枚举：缓存数据结构类型 |
| `CacheTopic` | 枚举：预定义缓存主题（含缓存名前缀和对应 `CacheType`） |

---

## CacheType 枚举

```java
public enum CacheType {
    MAP,    // 键值映射结构
    ARRAY,  // 数组/列表结构
}
```

---

## CacheTopic 枚举

`CacheTopic` 将缓存主题名称与数据类型绑定，构造时自动在名称末尾拼接 `:`，避免硬编码字符串。

```java
public enum CacheTopic {
    SYSTEM("system", CacheType.MAP);
    // cacheName = "system:"
    // cacheType = CacheType.MAP
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `cacheName` | `String` | 缓存名前缀，格式为 `<field>:`，如 `"system:"` |
| `cacheType` | `CacheType` | 该主题对应的数据结构类型 |

---

## BaseCache 接口（核心操作）

`LocalCacheContext` 和 `RemoteCacheContext` 均实现此接口：

```java
public interface BaseCache {
    Uni<Object> get(String key);
    Uni<Void> del(String key);
    Uni<Void> set(String key, Object v);
    Uni<List<String>> keys();
}
```

### RemoteCacheContext 额外方法

```java
// NX 语义写入（key 不存在时写入并设置过期时间，key 已存在时不覆盖）
Uni<Boolean> setExpireAfterWrite(String key, long seconds, Object value)
```

---

## 配置项

| 配置键 | 来源 | 说明 |
|--------|------|------|
| `quarkus.plugins.cache.enable` | `CacheService` `@IfBuildProperty` | 构建时开关，设为 `"true"` 激活 `CacheService` bean |
| `quarkus.application.name` | `CacheContextProvider` `@ConfigProperty` | Redis key 的应用名前缀，最终拼成 `<prefix>:<key>` |

> **前缀注入路径**：`RemoteCacheContext` 通过 `new` 构造、不是 CDI bean，因此 `prefix`
> 不能直接由 `@ConfigProperty` 注入。应用名在 CDI bean `CacheContextProvider` 中解析，
> 经 `CacheContext` 构造器透传给 `RemoteCacheContext` 的 `final prefix` 字段。

Quarkus Redis 连接本身由 `quarkus-redis-client` 扩展管理，相关配置键（如
`quarkus.redis.hosts`）请参考 Quarkus Redis 官方文档，不在本模块范围内。

---

## 使用示例

### 注入 CacheService

```java
@ApplicationScoped
public class OrderService {

    @Inject
    CacheService cacheService;

    public Uni<Void> cacheOrder(String orderId, Object orderData) {
        // 通过本地缓存写入
        return cacheService.getCacheContext()
                           .localCacheContext
                           .set(CacheTopic.SYSTEM.cacheName + orderId, orderData);
    }

    public Uni<Object> fetchOrder(String orderId) {
        // 从本地缓存读取
        return cacheService.getCacheContext()
                           .localCacheContext
                           .get(CacheTopic.SYSTEM.cacheName + orderId);
    }

    public Uni<Object> fetchRemoteOrder(String orderId) {
        // 从 Redis 读取
        return cacheService.getCacheContext()
                           .remoteCacheContext
                           .get(CacheTopic.SYSTEM.cacheName + orderId);
    }
}
```

### 使用 RemoteCacheContext.setExpireAfterWrite

```java
// 仅在 key 不存在时写入，并设置 60 秒过期（NX + EXPIRE 原子复合）
Uni<Boolean> result = cacheService.getCacheContext()
        .remoteCacheContext
        .setExpireAfterWrite("lock:resource:42", 60L, "locked");
```

### 继承 CacheStd 自定义服务

```java
@ApplicationScoped
public class SessionService extends CacheStd implements ICacheOperations {

    public Uni<Void> saveSession(String sessionId, Object sessionData) {
        return getCacheContext().remoteCacheContext
                                .set("session:" + sessionId, sessionData);
    }
}
```

---

## 注意事项

1. **Redis key 前缀**：`RemoteCacheContext` 不受 CDI 管理，`prefix` 不能直接由
   `@ConfigProperty` 注入；应用名在 `CacheContextProvider` 中解析后经构造器透传，最终
   Redis key 形如 `<quarkus.application.name>:<key>`。若未配置 `quarkus.application.name`，
   注入会失败（启动期报错），而非静默使用 `null` 前缀。

2. **本地缓存无过期设置**：`LocalCacheContext` 的 Caffeine 缓存配置了
   `initialCapacity(100)` 和 `maximumSize(100000)`，过期相关配置（`expireAfterWrite` /
   `expireAfterAccess`）在源码中已注释，当前无过期淘汰策略，仅依赖最大容量 LFU 淘汰。

3. **所有操作返回 `Uni`**：不提供同步阻塞方法，需在 Mutiny / Vert.x 响应式上下文中订阅，
   避免在 Quarkus EventLoop 线程上阻塞等待。

4. **`ICacheOperations` 当前无方法**：该接口目前是空标记接口，具体操作通过
   `getCacheContext().localCacheContext` 和 `getCacheContext().remoteCacheContext` 直接访问。

5. **构建开关变更须重新编译**：`quarkus.plugins.cache.enable=true` 是 `@IfBuildProperty` 构建期属性，
   运行时动态修改无效。
