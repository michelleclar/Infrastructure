# Quarkus 适配层解耦说明

本文档用于约束 `infrastructure-component-quarkus/*` 的职责边界。目标是让 Quarkus 子模块只承担框架适配和装配职责，组件核心能力下沉到可独立复用的 `infrastructure-component-*` 模块。

本次文档只定义拆分原则和任务清单，不直接移动源码。

## 目标定位

| 层级 | 模块形态 | 职责 |
|------|----------|------|
| 独立组件层 | `infrastructure-component-{name}` | core、api、implementation，可被任意 Java 项目使用，不依赖 Quarkus |
| Quarkus 适配层 | `infrastructure-component-quarkus:{name}` | CDI Bean、ConfigMapping、生命周期事件、JAX-RS、事务装配、Quarkus 扩展 API 适配 |
| 示例层 | `demo/` | 作为 Quarkus 应用示例，只演示接线方式 |

## 统一拆分原则

- core 模块不得出现 Quarkus、CDI、JAX-RS、MicroProfile Config 相关 import。
- core 模块可以依赖明确的业务或中间件 SDK，例如 jOOQ、Elasticsearch Java Client、Pulsar Client、Temporal SDK，但不能依赖 Quarkus 对这些 SDK 的封装。
- Quarkus adapter 负责把 core 能力注册成可注入 Bean，并处理配置、生命周期、事务边界和 Web 暴露。
- adapter 可以依赖 core，core 不得反向依赖 adapter。
- 能力接口优先放在 core，例如 `ISearchAbility`、`IAuditAbility`、`IUserIdentity`。只有确实绑定 Quarkus 上下文的接口才留在 adapter。
- 生成代码仍然遵循现有规则：`src/main/gen/` 禁止手动编辑。

core 层禁用依赖包前缀：

```text
io.quarkus.*
jakarta.enterprise.*
jakarta.inject.*
jakarta.ws.rs.*
org.eclipse.microprofile.config.*
io.smallrye.config.*
```

## P0 构建拆分任务

当前 `infrastructure-component-quarkus/build.gradle.kts` 的 `subprojects` 中仍然对所有子项目执行：

```kotlin
apply(plugin = "io.quarkus")
```

这会让所有 Quarkus 子模块无差别套上 Quarkus 插件，不利于后续将部分代码迁到独立 core 模块。P0 任务是调整构建约束：

- 父工程只保留通用发布、测试和依赖约束。
- 只有真正的 Quarkus adapter 子模块显式应用 `io.quarkus`。
- 后续新增的 `infrastructure-component-{name}` core 模块不得继承 Quarkus 插件、Quarkus BOM 或 Quarkus 测试配置。
- 对每个迁出的 core 模块补充单模块测试任务，例如 `./gradlew :infrastructure-component-search:test`。

## 优先级

| 优先级 | 模块 | 原因 |
|--------|------|------|
| P0 | Quarkus 构建脚本 | 先解除插件无差别 apply，避免 core 模块被 Quarkus 污染 |
| P1 | `search`、`approval`、`authorization` | 当前混合了较多核心模型、服务或 SDK 封装，拆分收益最高 |
| P2 | `workflow`、`cache`、`audit` | 已有部分独立能力或可复用模型，需要整理边界 |
| P3 | `mq`、`persistence`、`discover`、`broadcast`、`web`、`user`、`metrics` | 大体是 adapter 职责，重点做边界确认和少量下沉 |

## 模块说明

- [approval](modules/approval.md)
- [audit](modules/audit.md)
- [authorization](modules/authorization.md)
- [broadcast](modules/broadcast.md)
- [cache](modules/cache.md)
- [discover](modules/discover.md)
- [metrics](modules/metrics.md)
- [mq](modules/mq.md)
- [persistence](modules/persistence.md)
- [search](modules/search.md)
- [user](modules/user.md)
- [web](modules/web.md)
- [workflow](modules/workflow.md)

## 总体验收标准

- README 链接覆盖全部 13 个 `infrastructure-component-quarkus` 子模块。
- 每个模块文档包含当前定位、是否需要独立 core、建议独立模块名、adapter 保留内容、迁出内容、依赖边界、拆分任务清单和验收标准。
- 每个模块文档的“模块审查补充”用于逐模块过代码：先看该模块解决的问题、如何使用、当前依赖和注意事项，再决定是否需要拆 core 或修边界。
- 后续进入实现阶段时，每个拆分任务至少通过：
  - 独立 core 模块：`./gradlew :infrastructure-component-{name}:test`
  - Quarkus adapter 模块：`./gradlew :infrastructure-component-quarkus:{name}:test`
  - core 源码中搜索不到禁用 import 前缀。
