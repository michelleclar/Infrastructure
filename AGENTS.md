# Repository Guidelines

## Project Overview

多模块 Gradle 基础设施组件库（**库，非可运行应用**），Java 21，包名 `org.carl.infrastructure.*`。
为基于 Quarkus 的微服务提供可复用的通用能力。详细 API 用法见 `doc/AI_MODULE_GUIDE.md`。

模块分两类：

- **Quarkus 集成模块** — `infrastructure-component-quarkus/<submodule>/`，依赖 Quarkus 框架，通过 `quarkus.plugins.<name>.enable` 属性条件注册 Bean
- **独立库模块** — `infrastructure-component-*/` 顶层目录，不依赖 Quarkus，可在任意 Java 项目中使用

主要模块：

| 类别 | 模块 | 说明 |
|------|------|------|
| Quarkus | authorization | OIDC/Keycloak 身份认证与权限控制 |
| Quarkus | web | RESTful Web 服务层（quarkus-rest + Jackson） |
| Quarkus | persistence | jOOQ + JDBC 持久化 |
| Quarkus | mq | Pulsar 消息队列 |
| Quarkus | cache | 本地与分布式缓存 |
| Quarkus | discover | Consul + Stork 服务发现 |
| Quarkus | workflow | Temporal 工作流编排 |
| Quarkus | metrics | OpenTelemetry 监控 |
| Quarkus | broadcast | Vert.x 事件总线 |
| Quarkus | search | Elasticsearch 全文搜索 |
| 独立库 | dto | Command/Query/Response/PageQuery 基类 |
| 独立库 | log | 统一日志接口（自动适配 SLF4J/JBoss） |
| 独立库 | utils | 通用工具类（字符串、集合、DAG 等） |
| 独立库 | persistence-jooq | jOOQ 数据库操作封装 |
| 独立库 | redis | Redis 客户端（异步/同步、分布式锁、泛型序列化） |
| 独立库 | mq-api / mq-pulsar | 消息队列抽象接口 + Pulsar 实现 |
| 独立库 | statemachine | 状态机 |

`demo/` 目录包含可运行的 Quarkus 示例应用，是各组件接线方式的参考。

## Build & Test

```bash
./gradlew build                                         # 构建全部
./gradlew test                                          # 测试全部
./gradlew :infrastructure-component-redis:test          # 测试单模块
./gradlew :demo:build                                   # 构建 demo
```

Native 构建（跨平台，使用容器）：

```bash
./gradlew build \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true \
  -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21.0.5
```

Redis/DB 集成测试需要本地服务；环境变量 `JDBC_URL`、`JDBC_USER`、`JDBC_PASSWORD` 未设置时测试通过 `assumeTrue()` 自动跳过。

## Architecture

### Ability 接口（核心设计模式）

服务能力通过可组合的 mixin 接口暴露，而非直接注入服务：

- `IPersistenceAbility` — 提供对 `IPersistenceOperations` 的访问
- `IMetadataAbility extends IPersistenceAbility` — 增加 schema 内省（getMainTable、getPK 等）
- `ILogAbility`、`ISearchAbility`、`IBroadcastAbility` 等同理

在 context/service 类上实现这些接口，而不是直接依赖注入。

### PersistenceContext

jOOQ 的核心封装，位于 `infrastructure-component-persistence-jooq`：

- 查询（有返回值）：`ctx.get(dsl -> dsl.selectFrom(TABLE).fetch())`
- 变更（无返回值）：`ctx.run(dsl -> dsl.insertInto(TABLE).values(...).execute())`
- 异步变体：`ctx.fetchAsync(...)` / `ctx.executeAsync(...)` 返回 `CompletionStage`

### 生成代码

jOOQ 从 PostgreSQL schema 生成 DAO 和 Record 类（生成配置见 `infrastructure-component-quarkus/workflow/`）。`src/main/gen/` 下的文件禁止手动编辑。

## Coding Style

- 4 空格缩进，LF 换行，UTF-8（见 `.editorconfig`）
- Java 21 toolchain
- 包名 `org.carl.infrastructure.*`，类名 PascalCase，测试类 `*Test.java`
- **日志统一使用 `ILogger`**（`org.carl.infrastructure.logging`），禁止直接使用 SLF4J/JBoss Logger
- 测试放于 `src/test/java`，与生产代码同包路径

## Commit

格式：`:emoji: 简短描述`

| Emoji | 场景 |
|-------|------|
| `:sparkles:` | 新功能 |
| `:bug:` | 修复 |
| `:recycle:` | 重构 |
| `:memo:` | 文档 |
| `:white_check_mark:` | 测试 |

## Key APIs Quick Reference

| 需求 | 用法 |
|------|------|
| 日志 | `ILogger logger = LoggerFactory.getLogger(MyClass.class)` |
| 数据库查询 | `ctx.get(dsl -> dsl.selectFrom(TABLE).fetch())` |
| 数据库更新 | `ctx.run(dsl -> dsl.insertInto(TABLE).values(...).execute())` |
| Redis 读写 | `RedisClientFactory.create()` → `.setSync()` / `.getSync()` |
| Redis 泛型 | `client.getSync("key", MyClass.class)` 或 `client.getSync("key", new TypeReference<>() {})` |
| 分布式锁 | `client.getLock("key").tryLock(...)` |
| 发消息 | `IProducer<T>.sendMessage(value, builder -> builder.key(...))` |
| 状态机 | `StateMachineBuilderFactory.create(S, E, C).state(...).build()` |
| 单体响应 | `SingleEntityResponse.of(data)` |
| 列表响应 | `MultiEntityResponse.of(list)` |
| 分页响应 | `PageEntityResponse.of(...)` — 继承 `PageQuery`，内置 `pageSize`/`pageIndex`/`orderBy`/`getOffset()` |

## Configuration & References

- Quarkus/Keycloak 配置：`doc/Quarkus.md`
- 环境变量模板：`resources/env/`
- 详细 API 使用示例：`doc/AI_MODULE_GUIDE.md`
- 发布坐标：`org.carl:infrastructure-component-{module}:1.0-BATE`（Aliyun Maven）
