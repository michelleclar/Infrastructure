# persistence-jooq

## 模块定位

`infrastructure-component-persistence-jooq` 是 jOOQ 持久化 core，提供 `PersistenceContext`、能力接口、DSLContext 工厂、schema metadata 读取和 table builder。Quarkus persistence adapter 应依赖该模块完成数据访问能力装配。

## 核心能力

- `PersistenceContext`：统一查询、变更、异步执行入口。
- `IPersistenceOperations`、`IPersistenceProvider`。
- `IPersistenceAbility`、`IMetadataAbility`、`ITableCreateAbility`。
- table builder：`TableBuilder`、`Column`、`Index`、`TableBase`。
- runtime：`ConnectionProvider`、`DslContextFactory`、`JooqCustomContext`。
- metadata：`DatabaseMetadataReader`、`DBSchema`、`DBTable`、`DBColumn` 等。
- `SqlLoggerListener`：SQL 日志监听。

## 依赖边界

- 可以依赖 jOOQ、JDBC 和日志组件。
- 不依赖 Quarkus、Agroal、CDI、MicroProfile Config。
- Quarkus datasource 到 `DSLContext` 的装配属于 `infrastructure-component-quarkus:persistence`。

## 对外 API

- `ctx.get(dsl -> ...)`：有返回值查询。
- `ctx.run(dsl -> ...)`：无返回值变更。
- `ctx.fetchAsync(...)`、`ctx.executeAsync(...)`：异步变体。
- `DatabaseMetadataReader`：读取 schema/table/column/index 元数据。

## 典型使用场景

- 非 Quarkus 应用直接通过 jOOQ 操作数据库。
- Quarkus adapter 生产 `PersistenceContext` 后注入到业务能力。
- 工具任务读取数据库 schema 生成或校验表结构。

## 维护事项

- 所有通用 jOOQ 工具应进入本模块，而不是 Quarkus persistence。
- 集成测试依赖 `JDBC_URL`、`JDBC_USER`、`JDBC_PASSWORD`，未配置时应自动跳过。
- SQL 日志必须使用 `ILogger`，不直接使用 SLF4J/JBoss Logger。

## 测试验收

- `./gradlew :infrastructure-component-persistence-jooq:test` 通过。
- 无数据库环境时集成测试通过 assume 跳过。
- 源码中没有 Quarkus、Agroal、CDI、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：封装 jOOQ 常用执行方式、元数据读取和表结构构建，给不同运行时提供一致的数据库访问能力。

**如何使用**：拿到 `DSLContext` 后创建或注入 `PersistenceContext`，查询用 `ctx.get(dsl -> dsl.selectFrom(TABLE).fetch())`，变更用 `ctx.run(dsl -> dsl.insertInto(TABLE).values(...).execute())`，异步用 `fetchAsync(...)` 或 `executeAsync(...)`。

**当前依赖了什么**：`api(libs.bundles.persistence.jooq)`、`implementation(project(":infrastructure-component-log"))`，测试 runtime 使用 PostgreSQL driver。

**需要注意什么**：该模块可以依赖 jOOQ，但不能依赖 Quarkus datasource/Agroal。审查时重点看 SQL 日志是否使用 `ILogger`，集成测试是否在缺少 `JDBC_URL` 等环境变量时跳过。
