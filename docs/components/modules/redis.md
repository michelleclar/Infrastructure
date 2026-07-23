# redis

## 模块定位

`infra-component-redis` 是基于 Vert.x Redis client 的独立 Redis 封装，提供异步和同步 API、泛型序列化支持、基础 key 操作和连接配置。

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
- `RedisClient#pttl(...)`、`scan(...)`、`keys(...)`、`incr(...)`。

## 典型使用场景

- 非 Quarkus 服务直接访问 Redis。
- 缓存组件底层远程缓存实现。
- 分布式锁、计数器、TTL 查询等基础 Redis 能力。

## 维护事项

- `keys(prefix)` 使用 SCAN 汇总结果；大 keyspace 使用分页 `scan(...)`，Cluster 模式明确不支持跨节点 SCAN。
- 同步 API 基于异步 join，所有命令有默认 30 秒总超时。
- 工厂创建的 Vert.x 随客户端关闭，注入的共享 Vert.x 由调用方关闭。
- 分布式锁提供 `terminationFuture()` 锁丢失通知，但不提供 fencing token。

## 测试验收

- `./gradlew :infra-component-redis:test` 通过。
- Testcontainers 覆盖 Standalone、Sentinel、Cluster 和 Replication；Docker 不可用时容器测试跳过。
- 源码中没有 Quarkus、CDI、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：提供独立 Redis client，统一字符串、对象序列化、TTL、批量删除、计数器等基础操作，避免业务直接操作 Vert.x Redis API。

**如何使用**：通过 `RedisClientFactory.create(...)` 创建 `RedisClient`，异步调用 `get/set/del/pttl/keys/incr`，同步场景调用对应 `getSync/setSync/delSync`。复杂对象读取可使用函数转换或泛型反序列化能力。

**当前依赖了什么**：直接依赖 Vert.x Core / Redis Client `4.5.23` 与 Jackson `2.20.1`，不通过 Quarkus Redis 或 Quarkus Cache 传递依赖。

**需要注意什么**：`keys(prefix)` 会聚合全部 SCAN 结果，大结果集使用分页 `scan(...)`；同步方法基于 `join()`，异常会包装成 unchecked；强一致业务不能只依赖无 fencing token 的 Redis 租约锁。
