# infrastructure-component-persistence-jooq

> 基于 jOOQ 的持久化基础层。提供 `PersistenceContext`（对 `DSLContext` 的轻量封装，支持同步/异步查询）、数据库元数据读取器以及连接回调抽象，供上层仓库/DAO 类直接注入使用。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-persistence-jooq"))
}
```

该模块通过 `api(...)` 传递导出以下依赖（无需在消费方重复声明）：

| 依赖 | 说明 |
|---|---|
| `org.jooq:jooq` | jOOQ 核心，提供 `DSLContext` / `DSL` |
| `jakarta.transaction:jakarta.transaction-api` | `@Transactional` 注解 |
| `jakarta.xml.bind:jakarta.xml.bind-api` | XML 绑定（jOOQ 内部配置读取） |
| `org.apache.velocity:velocity-engine-core` | jOOQ 代码生成模板引擎（运行时传递） |
| `org.apache.commons:commons-lang3` | 字符串工具等 |

---

## 配置

本模块不自动托管 DataSource，也不读取任何框架配置键。使用方负责构造 `javax.sql.DataSource`（例如 Quarkus Agroal、HikariCP 等），然后手动传入 `PersistenceContext.create(dataSource)`。

如果与 Quarkus 一起使用，建议在同一 Gradle 模块额外引入：

```kotlin
implementation(libs.bundles.persistence.quarkus)   // quarkus-jdbc-postgresql + quarkus-agroal
```

并在 `application.properties` 中配置标准 Quarkus 数据源键：

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=...
quarkus.datasource.password=...
```

> 上述配置键属于 Quarkus Agroal，不属于本模块的 API。

---

## 核心 API

### 稳定 API（正常使用）

| 类 / 接口 | 包 | 说明 |
|---|---|---|
| `PersistenceContext` | `org.carl.infrastructure.persistence.core` | 核心入口；封装 `DSLContext`，提供同步/异步查询方法 |
| `IPersistenceProvider` | `org.carl.infrastructure.persistence` | 提供 `PersistenceContext dsl()` 和元数据读取 default 方法的接口，供 DAO 基类实现 |
| `IPersistenceOperations` | `org.carl.infrastructure.persistence` | 继承 `IPersistenceProvider`，补充 `persistenceCtx()` 和 `execute(String sql)` |
| `IPersistenceAbility` | `org.carl.infrastructure.persistence.ability` | 面向业务层的能力接口，声明 `getPersistenceOperations()` |
| `IMetadataAbility` | `org.carl.infrastructure.persistence.ability` | 继承 `IPersistenceAbility`；提供 `getMainTable()`、`getSchema()`、`getDBInfo()` default 方法 |
| `DslContextFactory` | `org.carl.infrastructure.persistence.engine.runtime` | 工厂方法，从 `DataSource` 构建 `DSLContext`，支持自动检测方言或手动指定 |
| `JooqCustomContext` | `org.carl.infrastructure.persistence.engine.runtime` | 接口；`apply(Configuration)` 插入自定义 jOOQ 配置（日志、超时等），默认实现已内置 `SqlLoggerListener` |
| `ConnectionProvider` | `org.carl.infrastructure.persistence.engine.runtime` | `Supplier<Connection>`，从 `DataSource` 获取原始连接 |
| `ConnectionCallable<T>` | `org.carl.infrastructure.persistence.function` | `@FunctionalInterface`，`T run(Connection) throws Throwable` |
| `ConnectionRunnable` | `org.carl.infrastructure.persistence.function` | `@FunctionalInterface`，`void run(Connection) throws Throwable` |
| `DatabaseMetadataReader` | `org.carl.infrastructure.persistence.metadata` | 运行时读取 JDBC + PostgreSQL catalog 元数据（schema/table/column/index/check constraint） |
| `SqlLoggerListener` | `org.carl.infrastructure.persistence.sql` | jOOQ `ExecuteListener`；在 TRACE 日志级别下打印实际 SQL 和参数（通过 logger 名 `org.carl.infrastructure.persistence.sql` 控制） |

### 已标记 `@Deprecated`（历史建表辅助，不推荐新代码使用）

| 类 | 说明 |
|---|---|
| `TableBuilder` | 对比 `DBInfo` 快照与目标 `TableWrapper`，执行增量 DDL（只支持 PostgreSQL） |
| `TableWrapper` | 描述目标表结构（列、索引、继承关系）的 Builder |
| `Column` / `ColumnType` | 列定义与列类型枚举 |
| `DBInfo` / `DBTable` / `DBColumn` / `DBSchema` / `DBIndex` / `DBIndexColumn` / `DBCheckConstraint` | 数据库结构快照模型 |
| `ITableCreateAbility` | 基于 `TableBuilder` 的建表接口（方法体大部分已注释） |

---

## `PersistenceContext` 方法速查

| 方法签名 | 说明 |
|---|---|
| `static PersistenceContext create(DataSource)` | 工厂方法，自动检测数据库方言 |
| `<T> T get(Function<DSLContext, T>)` | 同步查询，返回结果 |
| `void run(Consumer<DSLContext>)` | 同步执行，不返回结果 |
| `<T> T connectionResult(ConnectionCallable<T>)` | 获取原始 `Connection` 并返回结果 |
| `void connection(ConnectionRunnable)` | 获取原始 `Connection` 执行副作用 |
| `SQLDialect getDialect()` | 返回当前连接方言 |
| `CompletionStage<Result<R>> fetchAsync(Function<DSLContext, ResultQuery<R>>)` | 异步查询 |
| `CompletionStage<Result<R>> fetchAsync(..., Executor)` | 异步查询，指定 Executor |
| `CompletionStage<T> fetchAsync(..., Function<Result<R>, T> mapper)` | 异步查询并映射结果 |
| `CompletionStage<T> fetchAsync(..., mapper, Executor)` | 异步查询并映射，指定 Executor |
| `CompletionStage<Integer> executeAsync(Function<DSLContext, Query>)` | 异步执行 DML，返回影响行数 |
| `CompletionStage<Integer> executeAsync(..., Executor)` | 异步执行 DML，指定 Executor |

---

## 使用示例

### 1. 直接使用 `PersistenceContext`（纯 Java，无框架）

```java
import org.carl.infrastructure.persistence.core.PersistenceContext;

DataSource dataSource = /* 你的 DataSource */ null;
PersistenceContext ctx = PersistenceContext.create(dataSource);

// 同步查询
List<String> names = ctx.get(dsl ->
        dsl.resultQuery("select name from users where active = true")
           .fetch()
           .getValues("name", String.class));

// 异步查询（返回 CompletionStage）
ctx.fetchAsync(dsl -> dsl.resultQuery("select * from orders where status = 'PENDING'"))
   .thenAccept(result -> System.out.println("待处理订单数: " + result.size()));

// 原始连接操作
ctx.connection(conn -> {
    conn.createStatement().execute("LOCK TABLE orders IN EXCLUSIVE MODE");
});
```

### 2. 在业务 DAO 中实现 `IPersistenceOperations`

```java
import org.carl.infrastructure.persistence.IPersistenceOperations;
import org.carl.infrastructure.persistence.core.PersistenceContext;

public class OrderRepository implements IPersistenceOperations {

    private PersistenceContext persistenceContext;

    public OrderRepository(PersistenceContext persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    @Override
    public PersistenceContext dsl() {
        return persistenceContext;
    }

    @Override
    public void setPersistenceContext(PersistenceContext persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    @Override
    public void resetDBInfo() {
        // 通常留空或清缓存
    }

    public List<String> findPendingOrderIds() {
        return persistenceCtx().get(dsl ->
                dsl.resultQuery("select order_id from orders where status = 'PENDING'")
                   .fetch()
                   .getValues("order_id", String.class));
    }
}
```

### 3. 自定义 jOOQ 配置（覆盖 `JooqCustomContext`）

```java
import org.carl.infrastructure.persistence.engine.runtime.DslContextFactory;
import org.carl.infrastructure.persistence.engine.runtime.JooqCustomContext;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

JooqCustomContext myContext = configuration -> {
    // 调用默认配置
    JooqCustomContext.super.apply(configuration);
    // 追加自定义设置
    configuration.settings().setQueryTimeout(30);
};

DSLContext dsl = DslContextFactory.create(SQLDialect.POSTGRES, dataSource, myContext);
```

### 4. 运行时读取数据库元数据

```java
import org.carl.infrastructure.persistence.metadata.DatabaseMetadataReader;
import org.carl.infrastructure.persistence.metadata.DBTable;

DatabaseMetadataReader reader = new DatabaseMetadataReader(ctx);

// 读取整库快照
var dbInfo = reader.getDBInfo();
dbInfo.getSchemas().forEach((name, schema) ->
        System.out.println("schema: " + name));

// 读取单张表快照
DBTable table = reader.getTable("public", "orders");
if (table != null) {
    table.getPrimaryKeyColumns().forEach(System.out::println);
    table.getIndexList().forEach(idx -> System.out.println(idx.getName()));
}
```

---

## SQL 日志

`SqlLoggerListener` 在 jOOQ TRACE 日志级别下自动激活。通过日志框架（Quarkus 默认使用 JBoss Logging）控制：

```properties
# application.properties（Quarkus）
quarkus.log.category."org.carl.infrastructure.persistence.sql".level=TRACE
```

- DEBUG 级别：打印带内联参数的 SQL（超过 2000 字符的 bind value 自动截断）
- TRACE 级别：打印原始 SQL + 完整 bind value

---

## 注意事项

- `PersistenceContext` 不管理事务；需要事务时，在调用方用 `@Transactional` 或手动 `connection()` 管理。
- `DslContextFactory.create(DataSource)` 默认自动检测方言，需在构建时借用一次连接；高并发下确保连接池有空闲连接。
- `builder.*`、`metadata.DB*`、`ITableCreateAbility` 均已标记 `@Deprecated`，其增量建表逻辑仅支持 PostgreSQL，新项目应通过 Flyway 等迁移工具管理 schema，不应依赖这些类。
- 模块内置了一条 Flyway 迁移脚本 `src/main/resources/db/migration/V1__workflow_archiving.sql`，用于工作流归档场景（`workflow_instance` + `execution_record` 两张表），由消费方决定是否启用。

---

## 测试

集成测试需要真实 PostgreSQL，通过环境变量激活：

```bash
export PERSISTENCE_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/testdb
export PERSISTENCE_TEST_JDBC_USER=postgres
export PERSISTENCE_TEST_JDBC_PASSWORD=secret

./gradlew :infrastructure-component-persistence-jooq:test
```

未设置上述变量时，集成测试自动跳过（`assumeTrue`），不影响 CI。

---

## License

跟随项目主 LICENSE。
