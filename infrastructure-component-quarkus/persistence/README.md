# infrastructure-component-quarkus / persistence

> `infrastructure-component-persistence-jooq` 的 Quarkus CDI 适配层。
> 通过 `DSLContextProvider` 将 Agroal 数据源注入 jOOQ，向容器生产 `PersistenceContext` 和 `DSLContext` 两个 CDI bean；
> 通过 `PersistenceStd` / `PersistenceService` 提供可直接注入或继承的标准持久化组件。

---

## 与 `infrastructure-component-persistence-jooq` 的关系

本模块在 `build.gradle.kts` 中使用 `api(project(":infrastructure-component-persistence-jooq"))`，
因此消费方只需声明本模块的依赖，即可传递获得 jOOQ 层的全部公共类型：
`PersistenceContext`、`IPersistenceProvider`、`IPersistenceOperations`、`DslContextFactory` 等。

jOOQ 层的完整 API 说明（`PersistenceContext` 方法速查、元数据 API、SQL 日志配置等）
请参阅：[infrastructure-component-persistence-jooq/README.md](../../infrastructure-component-persistence-jooq/README.md)

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:persistence"))
}
```

本模块传递以下运行时依赖（消费方无需重复声明）：

| 依赖 | 说明 |
|---|---|
| `io.quarkus:quarkus-jdbc-postgresql` | PostgreSQL JDBC 驱动（Quarkus 集成） |
| `io.quarkus:quarkus-agroal` | Quarkus Agroal 连接池，提供 `AgroalDataSource` CDI bean |
| `infrastructure-component-persistence-jooq`（`api`） | jOOQ 持久化基础层（见上方说明） |

---

## 构建开关

`PersistenceService` bean 受编译期属性控制，**默认不激活**，需在 `application.properties` 中显式开启：

```properties
quarkus.plugins.persistence.enable=true
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `quarkus.plugins.persistence.enable` | `String` | （未设置） | 设为 `"true"` 时才将 `PersistenceService` 注册为 CDI bean |

> 这是 `@IfBuildProperty` 构建时开关，**更改后需重新编译**，运行期修改无效。
>
> `PersistenceStd`（`@ApplicationScoped`）和 `DSLContextProvider` 产生的两个 bean 不受此开关控制，始终存在于容器中。

---

## CDI 生产：`DSLContextProvider`

`DSLContextProvider`（`org.carl.infrastructure.persistence.core`）实现 `Provider<PersistenceContext>`，
注入 `AgroalDataSource`，向容器生产两个 `@ApplicationScoped` bean：

| `@Produces` 方法 | 返回类型 | 限定 | 实现 |
|---|---|---|---|
| `get()` | `PersistenceContext` | `@DefaultBean` | `PersistenceContext.create(dataSource)` |
| `getDSLContext()` | `DSLContext` | `@DefaultBean` | `DslContextFactory.create(dataSource)` |

两个 bean 均以 `@DefaultBean` 标注，允许消费方通过自定义 `@Produces` 方法覆盖。

数据源本身由 Quarkus Agroal 管理，配置键属于 Quarkus 标准 datasource API：

```properties
# application.properties（Quarkus 标准 datasource 配置键）
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=...
quarkus.datasource.password=...
```

> `DSLContextProvider` 本身不读取任何自定义配置键；上述键由 `quarkus-agroal` 解析。

---

## 核心类

### `PersistenceStd`

包：`org.carl.infrastructure.persistence`
作用域：`@ApplicationScoped`
实现：`IPersistenceProvider`

| 方法签名 | 说明 |
|---|---|
| `PersistenceContext dsl()` | 返回注入的 `PersistenceContext` |
| `void setPersistenceContext(PersistenceContext persistenceContext)` | `@Inject` 注入点，由容器调用 |
| `void resetDBInfo()` | 清除缓存的 `DBInfo`，下次访问时重新从数据库读取 |
| `DBInfo getDBInfo()` | 懒加载（`volatile` 单次缓存）获取数据库结构快照 |

`PersistenceStd` 不直接实现 `IPersistenceOperations`，不含 `persistenceCtx()` / `execute(String sql)` 等方法。

### `PersistenceService`

包：`org.carl.infrastructure.persistence`
作用域：`@Singleton`
继承：`PersistenceStd`
实现：`IPersistenceOperations`（继承自 `PersistenceStd` 并补充 `IPersistenceOperations`）
激活条件：`@IfBuildProperty(name = "quarkus.plugins.persistence.enable", stringValue = "true")`

`PersistenceService` 自身无额外方法体；全部方法由 `PersistenceStd` 和 `IPersistenceOperations` default 方法提供。

通过 `IPersistenceOperations` 获得的额外方法（来自 jOOQ 层）：

| 方法签名 | 说明 |
|---|---|
| `PersistenceContext persistenceCtx()` | 等同于 `dsl()`，语义上强调操作上下文 |
| `void execute(String sql)` | 执行原始 SQL，无返回值 |
| `Map<String, DBColumn> getColumnMap(String schema, String tableName)` | 按列名返回指定表的列元数据 |

（继承的 `IPersistenceProvider` default 方法）

| 方法签名 | 说明 |
|---|---|
| `DatabaseMetadataReader metadataReader()` | 构造元数据读取器 |
| `DBInfo getDBInfo()` | 读取整库结构快照（`PersistenceStd` 已加缓存覆盖） |
| `DBTable getDBTable(String schema, String tableName)` | 读取单张表快照 |

---

## EntityTemplate

路径：`src/main/resources/EntityTemplate.java`

这是一个 jOOQ 代码生成实体模板，**不是普通 Java 源文件**，不参与编译。
模板占位符为 `$className`、`$propertyName`、`$propertyType`，供代码生成器在生成实体类时替换。
消费方在 `build.gradle.kts` 中通过 `jooqGenerator(...)` 引用本模块，并配置生成器使用此模板即可。

---

## 使用示例

### 1. 直接注入 `DSLContext`

```java
import jakarta.inject.Inject;
import org.jooq.DSLContext;

@ApplicationScoped
public class OrderRepository {

    @Inject
    DSLContext dsl;

    public List<String> findPendingOrderIds() {
        return dsl.resultQuery("select order_id from orders where status = 'PENDING'")
                  .fetch()
                  .getValues("order_id", String.class);
    }
}
```

### 2. 注入 `PersistenceContext`（同步/异步双模式）

```java
import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.core.PersistenceContext;

@ApplicationScoped
public class OrderRepository {

    @Inject
    PersistenceContext ctx;

    // 同步查询
    public List<String> findActive() {
        return ctx.get(dsl ->
                dsl.resultQuery("select name from orders where active = true")
                   .fetch()
                   .getValues("name", String.class));
    }

    // 异步查询
    public CompletionStage<?> findPendingAsync() {
        return ctx.fetchAsync(dsl ->
                dsl.resultQuery("select * from orders where status = 'PENDING'"));
    }
}
```

### 3. 注入 `PersistenceService`（需开启构建开关）

```properties
# application.properties
quarkus.plugins.persistence.enable=true
```

```java
import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.PersistenceService;

@ApplicationScoped
public class OrderService {

    @Inject
    PersistenceService persistence;

    public void initTable() {
        persistence.execute("CREATE TABLE IF NOT EXISTS orders (id bigserial primary key)");
    }

    public Map<String, DBColumn> getColumns() {
        return persistence.getColumnMap("public", "orders");
    }
}
```

### 4. 业务 bean 继承 `PersistenceStd`

```java
import org.carl.infrastructure.persistence.PersistenceStd;

@ApplicationScoped
public class ProductRepository extends PersistenceStd {

    public List<String> findAll() {
        return dsl().get(ctx ->
                ctx.resultQuery("select name from products")
                   .fetch()
                   .getValues("name", String.class));
    }
}
```

---

## 配置项汇总

| 配置键 | 所属层 | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| `quarkus.plugins.persistence.enable` | 本模块 | `String` | （未设置） | 构建时开关；设为 `"true"` 激活 `PersistenceService` bean |
| `quarkus.datasource.db-kind` | `quarkus-agroal` | `String` | — | 数据库类型，如 `postgresql` |
| `quarkus.datasource.jdbc.url` | `quarkus-agroal` | `String` | — | JDBC 连接 URL |
| `quarkus.datasource.username` | `quarkus-agroal` | `String` | — | 数据库用户名 |
| `quarkus.datasource.password` | `quarkus-agroal` | `String` | — | 数据库密码 |

> `DSLContextProvider` 不读取除 `AgroalDataSource` 注入以外的任何自定义键；数据源配置键均属于 Quarkus Agroal。

---

## 注意事项

- `PersistenceContext` 和 `DSLContext` 两个 bean 以 `@DefaultBean` 生产，支持覆盖：在消费模块中声明自定义 `@Produces @ApplicationScoped` 方法即可替换默认实现。
- `PersistenceContext` 不管理事务；需要事务时，在调用方使用 `@Transactional` 或通过 `ctx.connection(...)` 手动管理。
- `PersistenceStd.getDBInfo()` 使用 `volatile` 字段做单次懒加载缓存；若数据库 schema 在运行期变更，需显式调用 `resetDBInfo()` 使缓存失效。
- `quarkus.plugins.persistence.enable` 是 `@IfBuildProperty` 编译期开关，运行期修改 `application.properties` 无效，必须重新编译。
- `EntityTemplate.java` 不参与编译，仅供 jOOQ 代码生成器使用，不应手动修改或直接引用。
