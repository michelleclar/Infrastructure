# persistence

## 当前定位

持久化已有独立 core 模块 `infrastructure-component-persistence-jooq`，包含 `PersistenceContext`、能力接口、metadata reader、table builder 和 jOOQ runtime 封装。`infrastructure-component-quarkus:persistence` 主要负责把 Quarkus `AgroalDataSource` 装配为 jOOQ `DSLContext` 和 `PersistenceContext`，整体方向基本正确。

## 是否需要独立 core

已存在，不需要新建。重点是防止 Quarkus adapter 重新承载 jOOQ core 能力。

## 建议独立模块名

沿用：`infrastructure-component-persistence-jooq`

## Quarkus adapter 应保留内容

- `DSLContextProvider` 中 `AgroalDataSource` 到 `DSLContext` 的 producer。
- `PersistenceService` 的 build property 条件注册。
- `PersistenceStd` 的 CDI 注入与能力暴露。
- Quarkus datasource、transaction 或 profile 相关装配。

## 需要迁出的核心能力

- jOOQ 查询执行、异步执行、metadata introspection、table builder 等全部保留或迁回 `persistence-jooq`。
- `IPersistenceOperations`、`IPersistenceProvider`、`IPersistenceAbility` 等能力接口保留在 core。
- 新增 SQL 日志、连接管理或 schema 工具时优先放 core，只有 Quarkus datasource 适配放 adapter。

## 依赖边界

- `persistence-jooq` 可以依赖 jOOQ 和 JDBC，但不得依赖 Quarkus、Agroal、CDI、MicroProfile Config。
- Quarkus persistence 可以依赖 Agroal、Quarkus Arc 和 `persistence-jooq`。
- 业务模块应依赖 `IPersistenceAbility` 或 `PersistenceContext`，避免直接依赖 Quarkus provider。

## 拆分任务清单

- 审查 Quarkus persistence 中是否新增了 core 查询或 metadata 逻辑。
- 保持 `DSLContextProvider` 只做 datasource 到 jOOQ 的适配。
- 补充 adapter 测试，验证 producer 可创建 `DSLContext` 和 `PersistenceContext`。
- 在文档中明确后续 jOOQ 工具类必须进入 `persistence-jooq`。

## 验收标准

- `./gradlew :infrastructure-component-persistence-jooq:test` 通过。
- `./gradlew :infrastructure-component-quarkus:persistence:test` 通过。
- `persistence-jooq` 源码中没有 Quarkus、CDI、MicroProfile Config import。
- Quarkus persistence 不包含通用 SQL builder、metadata reader 或 repository 基类。

## 模块审查补充

**解决的问题**：把 Quarkus 数据源 `AgroalDataSource` 装配成 jOOQ `DSLContext` 和 `PersistenceContext`，让业务通过统一 persistence ability 执行查询和变更。

**如何使用**：依赖 `infrastructure-component-quarkus:persistence`，配置 `quarkus.plugins.persistence.enable=true` 以及 Quarkus datasource。业务中注入 `PersistenceContext`、`DSLContext` 或实现 `IPersistenceAbility` 后使用 `ctx.get(...)`、`ctx.run(...)`。

**当前依赖**：`api(project(":infrastructure-component-persistence-jooq"))`、`implementation(libs.bundles.persistence.quarkus)`。

**需要注意**：Quarkus persistence 不应包含 repository 通用基类、metadata reader 或 SQL builder。审查时只允许 datasource/DSLContext/PersistenceContext producer、开关 Bean 和 Quarkus 事务接线留在 adapter。
