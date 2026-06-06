# search

## 当前定位

`infrastructure-component-quarkus:search` 当前包含 Elasticsearch context、provider、ability、查询构建、mapping 构建、action 封装和 Quarkus Bean 注册。大量能力与 Quarkus 无关，应作为独立搜索组件。

## 是否需要独立 core

需要。搜索组件的 ES client 封装、查询构建和 action API 可被非 Quarkus 项目复用。

## 建议独立模块名

`infrastructure-component-search`

如果未来需要多搜索引擎，可进一步拆分：

- `infrastructure-component-search-api`
- `infrastructure-component-search-elasticsearch`

## Quarkus adapter 应保留内容

- `ESContextProvider` CDI producer。
- `ESService` 上的 build property 条件注册。
- Quarkus 中 Elasticsearch client Bean 的注入和 core `ESContext` 创建。
- 将 `ISearchAbility` 暴露为 Quarkus 可注入能力的装配代码。

## 需要迁出的核心能力

- `ESContext`。
- `IESOperations`、`IESProvider`、`ISearchAbility`。
- `Mapping`、`MultiMatchQuery`、`PropertyType`、`Query` 等构建器。
- `Delete`、`Get`、`Index`、`Indices`、`Search`、`Update` 等 action 封装。
- `ElasticsearchClientToolUtils`。
- 与 Elasticsearch Java Client 相关但不依赖 Quarkus 的异常和工具封装。

## 依赖边界

- search core 可以依赖 Elasticsearch Java Client。
- search core 不得依赖 Quarkus、CDI、JAX-RS、MicroProfile Config。
- Quarkus search 可以依赖 Quarkus Elasticsearch extension、Arc、core search 模块。
- 如果引入 search api，api 层不应泄漏 Elasticsearch 具体 request/response 类型，ES 实现模块再依赖 Elasticsearch client。

## 拆分任务清单

- 创建 `infrastructure-component-search`。
- 迁出 ES context、operation、provider、query builder、action 封装和工具类。
- 将 Quarkus search 改为只生产 `ESContext` 并注册能力 Bean。
- 评估 `ISearchAbility` 是否应保持 ES 类型暴露，或拆出更通用的 search api。
- 为 mapping 构建、multi match query、index/search/get/update/delete action 增加 core 单元测试。

## 验收标准

- `./gradlew :infrastructure-component-search:test` 通过。
- `./gradlew :infrastructure-component-quarkus:search:test` 通过。
- search core 源码中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。
- Quarkus search 中不再保留查询构建和 ES action 业务封装。

## 模块审查补充

**解决的问题**：封装 Elasticsearch client、index/search/get/update/delete action 和查询/mapping 构建能力，给业务提供统一搜索操作入口。

**如何使用**：在 Quarkus 应用中依赖 `infrastructure-component-quarkus:search`，配置 `quarkus.plugins.search.enable=true`，并按 Quarkus Elasticsearch client 方式配置连接。业务侧通过 `ISearchAbility`、`IESOperations` 或 `ESContext` 执行 ES action。

**当前依赖**：`implementation(libs.bundles.search)`；源码使用 Elasticsearch Java Client、CDI 和 `@IfBuildProperty`。

**需要注意**：ES action 封装、mapping/query builder 与 Quarkus 无关，应迁到 search core。审查时如果看到 `co.elastic.clients.*` 和 `jakarta.inject.*` 在同一能力链路里混用，要拆成 core client/action 与 Quarkus provider 两层。
