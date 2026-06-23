# 独立组件说明

本文档覆盖 `infrastructure-component-quarkus/` 之外的顶层 `infrastructure-component-*` 组件。与 [Quarkus 适配层解耦说明](../quarkus-decoupling/README.md) 配套使用：这里描述可独立复用的 core/API/implementation 组件，Quarkus 目录只负责框架适配。

## 组件清单

| 组件 | 构建状态 | 定位 |
|------|----------|------|
| `infrastructure-component-dto` | 已 include | Command/Query/Response/PageQuery 基类 |
| `infrastructure-component-log` | 已 include | 统一日志接口与 SLF4J/JBoss 适配 |
| `infrastructure-component-utils` | 已 include | 字符串、集合、脱敏、DAG 等工具 |
| `infrastructure-component-http` | 已 include | 独立 HTTP client 抽象与 Apache 实现 |
| `infrastructure-component-mq-api` | 已 include | MQ producer/consumer/client 抽象 |
| `infrastructure-component-mq-pulsar` | 已 include | Pulsar MQ API 实现 |
| `infrastructure-component-workflow-core` | 已 include | 纯 Java 工作流定义、DSL、NodeHandler SPI 与图校验 |
| `infrastructure-component-workflow-temporal` | 已 include | 基于 Temporal 的工作流运行时适配 |
| `infrastructure-component-persistence-jooq` | 已 include | jOOQ 持久化上下文与元数据工具 |
| `infrastructure-component-redis` | 已 include | Vert.x Redis 客户端封装 |
| `infrastructure-component-rule-engine` | 已 include | 轻量规则引擎 |
| `infrastructure-component-pdp` | 已 include | 策略决策点 |
| `infrastructure-component-statemachine` | 已 include | 状态机 builder 和运行时 |
| `infrastructure-component-qdrant-grpc` | 已 include | Qdrant gRPC client 与 request factory |
| `infrastructure-component-embedding-grpc` | 已 include | Embedding gRPC client |
| `infrastructure-component-audit` | 目录存在，未 include | 历史审计 core，需要与 Quarkus audit 合并整理 |

## 边界规则

- 独立组件不应依赖 `infrastructure-component-quarkus:*`。
- 独立组件如需要框架接线，应拆成 core 与 adapter，adapter 放入 `infrastructure-component-quarkus/<name>` 或明确命名的框架模块。
- `dto`、`utils`、`log` 是底层基础模块，不应反向依赖业务型组件。
- `mq-api`、`pdp`、`rule-engine`、`statemachine` 属于抽象或纯逻辑组件，优先保持无框架依赖。
- `mq-pulsar`、`redis`、`persistence-jooq`、`qdrant-grpc`、`embedding-grpc` 可以依赖对应中间件 SDK，但不应混入 Quarkus/CDI 配置读取。
- 所有组件对外暴露的异常、配置和能力接口应稳定，避免让上层业务依赖实现类细节。

## 文档链接

- [audit](modules/audit.md)
- [dto](modules/dto.md)
- [embedding-grpc](modules/embedding-grpc.md)
- [http](modules/http.md)
- [log](modules/log.md)
- [mq-api](modules/mq-api.md)
- [mq-pulsar](modules/mq-pulsar.md)
- [workflow-core](modules/workflow-core.md)
- [workflow-temporal](modules/workflow-temporal.md)
- [pdp](modules/pdp.md)
- [persistence-jooq](modules/persistence-jooq.md)
- [qdrant-grpc](modules/qdrant-grpc.md)
- [redis](modules/redis.md)
- [rule-engine](modules/rule-engine.md)
- [statemachine](modules/statemachine.md)
- [utils](modules/utils.md)

## 通用验收

- 每个已 include 组件至少应能运行对应单模块测试：`./gradlew :infrastructure-component-{name}:test`。
- 不在 `settings.gradle.kts` include 的目录应标明原因，避免误以为仍在发布链路中。
- 独立组件说明文档需要覆盖模块定位、核心能力、依赖边界、对外 API、典型使用场景、维护事项和测试验收。
- 过模块时优先阅读每篇文档的“使用与依赖补充”：它明确该模块为了解决什么、如何使用、当前依赖了什么，以及审查时最需要注意的拆分信号。
