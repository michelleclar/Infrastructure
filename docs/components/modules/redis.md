# redis

## 模块定位

`infrastructure-component-redis` 是基于 Vert.x Redis client 的独立 Redis 封装，提供异步和同步 API、泛型序列化支持、基础 key 操作和连接配置。

## 核心能力

- `RedisClientFactory`：创建 Redis client。
- `RedisClient`：封装 get/set/del/pttl/keys/incr 等操作。
- `RedisConfigOptions`：Redis 连接配置。
- `SentinelRole`、`SentinelType`：哨兵配置枚举。
- Jackson JSR310 时间类型支持。

## 依赖边界

- 可以依赖 Vert.x Redis client、Vert.x core、Jackson。
- 不依赖 Quarkus Redis datasource、CDI、MicroProfile Config。
- Quarkus cache adapter 如需 Redis，应复用本模块或做 Quarkus Redis 到本模块抽象的桥接。

## 对外 API

- `RedisClientFactory.create(...)`。
- `RedisClient#get(...)`、`getSync(...)`。
- `RedisClient#set(...)`、`setSync(...)`。
- `RedisClient#del(...)`、`delSync(...)`。
- `RedisClient#pttl(...)`、`keys(...)`、`incr(...)`。

## 典型使用场景

- 非 Quarkus 服务直接访问 Redis。
- 缓存组件底层远程缓存实现。
- 分布式锁、计数器、TTL 查询等基础 Redis 能力。

## 维护事项

- `keys(prefix)` 使用 Redis `KEYS` 命令，生产大 keyspace 场景应谨慎，必要时增加 scan API。
- 同步 API 基于异步 join，应明确异常包装行为。
- 连接关闭要释放 Vert.x 和 Redis 资源，避免测试或短生命周期任务泄漏。

## 测试验收

- `./gradlew :infrastructure-component-redis:test` 通过。
- Redis 集成测试在无本地 Redis 时应可跳过或明确失败原因。
- 源码中没有 Quarkus、CDI、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：提供独立 Redis client，统一字符串、对象序列化、TTL、批量删除、计数器等基础操作，避免业务直接操作 Vert.x Redis API。

**如何使用**：通过 `RedisClientFactory.create(...)` 创建 `RedisClient`，异步调用 `get/set/del/pttl/keys/incr`，同步场景调用对应 `getSync/setSync/delSync`。复杂对象读取可使用函数转换或泛型反序列化能力。

**当前依赖了什么**：`api(libs.bundles.cache)`，实现使用 Vert.x Redis client；另依赖 `jackson-datatype-jsr310:2.18.2` 支持 Java 时间类型。

**需要注意什么**：`keys(prefix)` 使用 Redis `KEYS`，生产环境大 keyspace 有阻塞风险，应考虑补 `SCAN`。同步方法基于 `join()`，异常会包装成 unchecked，需要在调用方明确处理。
