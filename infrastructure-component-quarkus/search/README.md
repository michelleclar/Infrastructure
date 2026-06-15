# infrastructure-component-quarkus:search

基于 `io.quarkus:quarkus-elasticsearch-java-client` 的 Elasticsearch 操作封装。通过分层接口将原生 ES Java API Client 的 Builder 调用简化为链式 fluent DSL，提供文档索引、按 ID 查询/删除/更新、全文搜索，以及索引管理（创建/删除）等能力。CDI bean `ESService` 在 Quarkus 容器中自动装配，业务类实现 `ISearchAbility` 接口即可直接调用所有操作。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:search"))
}
```

底层 ES 客户端库：

| 依赖坐标 | 版本 |
|---|---|
| `io.quarkus:quarkus-elasticsearch-java-client` | `3.30.4`（跟随 Quarkus BOM） |

---

## 架构分层

```
ISearchAbility                    ← 业务层接口（ability/）
    │  getSearchOperations()
    ▼
IESOperations                     ← ES 操作接口（plugins/es/）
    │  extends IESProvider
    │  default 方法直接 new Action 对象
    ▼
ESStd                             ← 持有 ESContext 的基类（plugins/es/）
    │  @Inject ESContext
    ▼
ESService                         ← CDI bean（@ApplicationScoped，plugins/es/）
    │  条件激活：quarkus.plugins.search.enable=true
    ▼
ESContext / ESContextProvider     ← 持有 ElasticsearchClient（plugins/es/core/）
    ▼
Action classes                    ← 具体操作链（plugins/es/core/action/）
    Index / Update / Search / Get / Delete / Indices
```

---

## 配置

`ESService` 通过以下构建期属性控制激活，配置在 `application.properties`（或 `application.yaml`）中：

| 配置键 | 类型 | 必须值 | 说明 |
|---|---|---|---|
| `quarkus.plugins.search.enable` | String | `"true"` | 仅当该值为 `true` 时，`ESService` CDI bean 才被注册 |

ES 连接本身（主机、端口、认证等）由 `io.quarkus:quarkus-elasticsearch-java-client` 扩展读取标准 Quarkus ES 配置键（如 `quarkus.elasticsearch.hosts`），不在本模块内定义。`ESContextProvider` 直接注入 Quarkus 扩展已构造好的 `ElasticsearchClient` bean，无需额外配置。

---

## 核心类

| 类 / 接口 | 包 | 说明 |
|---|---|---|
| `ISearchAbility` | `ability/` | 业务层接口，提供 5 个 default 方法，委托给 `IESOperations` |
| `IESProvider` | `plugins/es/` | 持有 `ESContext` 的最小接口 |
| `IESOperations` | `plugins/es/` | extends `IESProvider`，default 方法创建 Action 对象 |
| `ESStd` | `plugins/es/` | `@Inject ESContext`，实现 `IESProvider` |
| `ESService` | `plugins/es/` | `@ApplicationScoped` CDI bean，extends `ESStd` implements `IESOperations` |
| `ESContext` | `plugins/es/core/` | 包装 `ElasticsearchClient` |
| `ESContextProvider` | `plugins/es/core/` | `@Produces @ApplicationScoped`，生产 `ESContext` |
| `Index<T>` | `plugins/es/core/action/` | 索引（写入）文档的链式操作 |
| `Update<T,K>` | `plugins/es/core/action/` | 更新/Upsert 文档的链式操作 |
| `Search` | `plugins/es/core/action/` | 搜索文档的链式操作 |
| `Get` | `plugins/es/core/action/` | 按 ID 读取文档的链式操作 |
| `Delete` | `plugins/es/core/action/` | 按 ID 删除文档的链式操作 |
| `Indices` | `plugins/es/core/action/` | 索引管理（创建/删除） |
| `Query` | `plugins/es/build/` | JSON 查询构建器（TermQuery / MultiMatchQuery） |
| `MultiMatchQuery` | `plugins/es/build/` | multi_match 查询构建器 |
| `Mapping` | `plugins/es/build/` | 索引 Mapping JSON 构建器 |
| `PropertyType` | `plugins/es/build/` | Mapping 字段类型枚举：`KEYWORD` / `TEXT` |
| `ElasticsearchClientToolUtils` | `plugins/es/core/utils/` | 工具类，`hitsToList` 将 `List<Hit<T>>` 展开为 `List<T>` |

---

## ISearchAbility API

业务 bean 实现此接口，覆写 `getSearchOperations()` 返回注入的 `ESService`，即可使用以下 default 方法：

```java
public interface ISearchAbility {
    IESOperations getSearchOperations();

    default <T> Index<T>      index(IndexRequest.Builder<T> builder);
    default     Search         search(SearchRequest.Builder builder);
    default     Get            get(GetRequest.Builder builder);
    default <T, K> Update<T, K> update(UpdateRequest.Builder<T, K> builder);
    default     Delete         delete(DeleteRequest.Builder builder);
}
```

---

## Action 操作链

每个 Action 类均采用内部静态类实现 fluent 链式调用，末尾调用 `executor()` 或 `fetch*()`触发实际 IO，内部统一将 `IOException` 包装为 `RuntimeException`。

### Index — 写入文档

```
Index<T>.index(String indexName)        → Index.Query<T>
    .id(String value)                   → Index.Doc<T>
    .document(T doc)                    → Index.Executor<T>
    .executor()                         → IndexResponse
```

### Update — 更新/Upsert 文档

```
Update<T,K>.index(String indexName)     → Update.Query<T,K>
    .id(String value)                   → Update.Action<T,K>
    .upsert(K doc)                      → Update.Executor<T,K>
    .executor()                         → UpdateResponse<T>
```

文档在 `.upsert(doc)` 处传入：内部对请求设置 `doc(doc)` 与 `docAsUpsert(true)`，语义为「目标文档存在则按 `doc` 部分更新，不存在则按 `doc` 整条插入」。泛型 `<T>` 为文档类型（用于响应反序列化），`<K>` 为写入文档类型，常见用法两者相同（如 `UpdateRequest.Builder<Article, Article>`）。

### Search — 搜索

```
Search.index(String indexName)          → Search._Query
    .query(Function<Query.Builder, ObjectBuilder<Query>> fn)  → Search.Fetch
    .fetch(Class<T> clazz)              → SearchResponse<T>
    .fetchOf(Class<T> clazz)            → List<T>            // 直接展开 hits
```

`fetchOf` 内部调用 `ElasticsearchClientToolUtils.hitsToList()`，直接返回 source 列表。

### Get — 按 ID 读取

```
Get.index(String indexName)             → Get.Query
    .id(String value)                   → Get.Fetch
    .fetchOf(Class<T> clazz)            → T                  // 返回 response.source()
```

### Delete — 按 ID 删除

```
Delete.index(String indexName)          → Delete.Query
    .id(String value)                   → Delete.Executor
    .executor()                         → DeleteResponse
```

### Indices — 索引管理

`Indices` 接受 `ElasticsearchIndicesClient`（通过 `ESContext.getClient().indices()` 获取）：

```java
// 创建索引（已存在时静默返回 null）
CreateIndexResponse create(String indexName)

// 按 Function Builder 创建索引（可携带 mapping、settings）
CreateIndexResponse create(Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> fn)

// 删除索引（先判断存在性：不存在时静默返回 null，存在才执行删除——幂等）
DeleteIndexResponse delete(String indexName)
```

---

## 查询构建器（build 包）

### Query

`Query` 是顶层容器，持有构建完毕的 `ObjectNode query` 和可选的分页参数。

```java
// 入口
Query q = Query.Q();

// 设置分页（from, size）
q.Page(Integer frome, Integer size);

// 进入 TermQuery 构建器
Query.TermQuery tq = q.TermQueryBuild()
    .setTermPair("status", "active")   // 设置字段名和值
    .build();                          // 写回到 q，返回 Query

// 进入 MultiMatchQuery 构建器
Query mq = q.MultiMatchQueryBuild()
    .setQuery("keyword")               // 搜索文本
    .setFields("title", "content")     // 目标字段
    .build();                          // 返回 Query
```

`Query.TermQuery` 生成的 JSON 节点格式：`{ "term": { "<field>": "<value>" } }`

`MultiMatchQuery` 生成的 JSON 节点格式：`{ "multi_match": { "query": "...", "fields": [...] } }`

> 注意：`build` 包的构建器产出原始 JSON `ObjectNode`，当前 Action 层使用的是 ES Java API Client 的 `Function<Query.Builder, ObjectBuilder<Query>>` 函数式接口，两套 API 并不直接互转，`build` 包主要用于手动拼接 JSON DSL 场景。

### Mapping

```java
String mappingJson = Mapping.build()
    .properties(p -> p
        .setName("title")
        .setType(PropertyType.TEXT)
        .build())
    .properties(p -> p
        .setName("status")
        .setType(PropertyType.KEYWORD)
        .build())
    .toString();   // 输出 { "properties": { "title": {"type":"text"}, "status": {"type":"keyword"} } }
```

`PropertyType` 枚举值：`KEYWORD`、`TEXT`（序列化为小写字符串）。

---

## 使用示例

### 1. 激活配置

```properties
# application.properties
quarkus.plugins.search.enable=true
quarkus.elasticsearch.hosts=localhost:9200
```

### 2. 业务 bean 实现 ISearchAbility

```java
@ApplicationScoped
public class ArticleSearchService implements ISearchAbility {

    @Inject
    ESService esService;

    @Override
    public IESOperations getSearchOperations() {
        return esService;
    }
}
```

### 3. 写入文档

```java
IndexResponse response = articleSearchService
    .index(new IndexRequest.Builder<Article>())
    .index("articles")
    .id("doc-001")
    .document(new Article("Java 入门", "active"))
    .executor();
```

### 4. 按 ID 读取文档

```java
Article article = articleSearchService
    .get(new GetRequest.Builder())
    .index("articles")
    .id("doc-001")
    .fetchOf(Article.class);
```

### 5. 搜索（ES Java API Client lambda）

```java
List<Article> results = articleSearchService
    .search(new SearchRequest.Builder())
    .index("articles")
    .query(q -> q.match(m -> m.field("title").query("Java")))
    .fetchOf(Article.class);
```

### 6. Upsert 文档

```java
Article article = new Article("Java 入门", "active");

UpdateResponse<Article> response = articleSearchService
    .update(new UpdateRequest.Builder<Article, Article>())
    .index("articles")
    .id("doc-001")
    .upsert(article)   // 存在则按 article 部分更新，不存在则插入
    .executor();
```

### 7. 删除文档

```java
DeleteResponse response = articleSearchService
    .delete(new DeleteRequest.Builder())
    .index("articles")
    .id("doc-001")
    .executor();
```

### 8. 创建索引（携带 Mapping）

```java
// 先用 Mapping 构建器生成 JSON（可作为参考），再通过 Indices 操作
Indices indices = new Indices(esService.getESContext().getClient().indices());

indices.create(c -> c
    .index("articles")
    .mappings(m -> m
        .properties("title",  p -> p.text(t -> t))
        .properties("status", p -> p.keyword(k -> k))
    )
);
```

---

## 注意事项

1. **条件激活**：`ESService` 使用 `@IfBuildProperty(name = "quarkus.plugins.search.enable", stringValue = "true")`，该注解在构建期生效。若属性未设置或值不为 `"true"`，CDI 容器中不存在此 bean，注入点会在启动时报错。

2. **IOException 处理**：所有 Action 类的 `executor()` / `fetch*()` 方法内部将 `IOException` 包装为 `RuntimeException` 抛出，调用方无需声明受检异常，但需在适当层级处理运行时异常。

3. **`Indices.create(String)` / `Indices.delete(String)` 的静默策略**：索引已存在（`create`）或索引 `already exists` 错误（`delete`）时返回 `null`，而非抛出异常。其他 `ElasticsearchException` 仍会上抛。

4. **`build` 包与 ES Java API Client 不直接互通**：`Query` / `MultiMatchQuery` 产出原始 JSON `ObjectNode`，Action 层的 `Search._Query.query(...)` 接受的是 ES Java API Client 的函数式接口 `Function<Query.Builder, ObjectBuilder<Query>>`，两者不能直接混用。

5. **线程安全**：`ESService` 是 `@ApplicationScoped` 单例，`ElasticsearchClient` 由 Quarkus 扩展管理，线程安全由底层客户端保证。

---

## License

跟随项目主 LICENSE。
