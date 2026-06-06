# cache

## 当前定位

`infrastructure-component-quarkus:cache` 同时包含缓存接口、缓存上下文、本地/远程缓存类型、Quarkus CacheManager 和 Quarkus Redis datasource 适配。缓存抽象与 Quarkus 实现边界需要拆清。

## 是否需要独立 core

需要。缓存能力至少应拆成通用缓存 API 和具体实现。远程 Redis 能力优先复用已有 `infrastructure-component-redis`。

## 建议独立模块名

- `infrastructure-component-cache-api`
- 可选：`infrastructure-component-cache-local`
- Redis 相关优先复用 `infrastructure-component-redis`

## Quarkus adapter 应保留内容

- `CacheManager`、`ReactiveRedisDataSource` 的注入和适配。
- `CacheContextProvider` CDI producer。
- `quarkus.plugins.cache.enable` 开关注册。
- Quarkus Cache、Quarkus Redis 与 core cache API 的桥接实现。

## 需要迁出的核心能力

- `ICacheOperations`、`ICacheProvider` 等缓存抽象。
- `CacheType`、`CacheTopic` 等与运行时无关的类型。
- 不依赖 Mutiny 或 Quarkus Redis 的缓存上下文接口。
- 本地缓存或远程缓存的通用策略定义。

## 依赖边界

- core/api 层不得依赖 `io.quarkus.cache.*`、`io.quarkus.redis.*`、`io.smallrye.mutiny.*`。
- 如果异步接口需要保留，core 优先使用 JDK `CompletionStage`。
- Quarkus adapter 可以依赖 Quarkus Cache、Quarkus Redis 和 Mutiny，并负责类型转换。
- Redis 客户端能力应复用 `infrastructure-component-redis`，避免重复实现。

## 拆分任务清单

- 创建 cache api 模块，迁出缓存接口和运行时无关类型。
- 梳理现有 `CacheContext` 中本地缓存与 Redis 操作，拆分为接口和 Quarkus 实现。
- 将 `ReactiveRedisDataSource` 相关代码保留在 Quarkus adapter。
- 将可复用 Redis 操作迁移或委托到 `infrastructure-component-redis`。
- 为缓存 get/set/delete、过期时间、命名空间等核心语义补充单元测试。

## 验收标准

- `./gradlew :infrastructure-component-cache-api:test` 通过。
- `./gradlew :infrastructure-component-quarkus:cache:test` 通过。
- cache api 模块没有 Quarkus、CDI、Mutiny、JAX-RS、MicroProfile Config import。
- Quarkus cache 模块只负责缓存 API 到 Quarkus Cache/Redis 的适配。

## 模块审查补充

**解决的问题**：为 Quarkus 服务提供本地缓存和远程 Redis 缓存访问入口，统一 get/set/delete/keys 等缓存操作。

**如何使用**：依赖 `infrastructure-component-quarkus:cache` 后注入 `ICacheProvider`、`ICacheOperations` 或 `CacheContext`。本地缓存通过 Caffeine，远程缓存通过 Quarkus `ReactiveRedisDataSource`。远程 key 当前按 `quarkus.application.name` 作为前缀。

**当前依赖**：该子模块自身 build 文件当前没有显式依赖，父工程会套 Quarkus BOM 和测试依赖；源码直接使用 Quarkus Cache、Quarkus Redis datasource、Caffeine、Mutiny、MicroProfile Config。

**需要注意**：`RemoteCacheContext.prefix` 是内部类字段上的 `@ConfigProperty`，这类注入在普通 `new RemoteCacheContext(...)` 场景下可能不会生效，需要重点验证。抽象层不应暴露 Mutiny 或 Quarkus Redis 类型；生产环境慎用 `keys()`，大 keyspace 下会有风险。
