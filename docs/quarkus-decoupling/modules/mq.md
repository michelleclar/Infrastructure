# mq

## 当前定位

MQ 已经存在独立模块 `infrastructure-component-mq-api` 和 `infrastructure-component-mq-pulsar`。`infrastructure-component-quarkus:mq` 主要包含 Quarkus 配置映射、Pulsar client producer 和生命周期关闭逻辑，整体方向基本正确。

## 是否需要独立 core

已存在 core/api 和 Pulsar 实现模块，不需要新建 core。当前重点是确保 Quarkus mq 不重复承载 Pulsar 核心实现。

## 建议独立模块名

沿用：

- `infrastructure-component-mq-api`
- `infrastructure-component-mq-pulsar`

## Quarkus adapter 应保留内容

- `MsgArgsConfig` 等 Quarkus/SmallRye config mapping。
- `PulsarClientProvide` CDI producer。
- `PulsarLifecycle` 对 client、producer、consumer 等资源的启动和关闭。
- Quarkus 配置对象到 `MQConfig` 或 `PulsarConfig` 的转换。

## 需要迁出的核心能力

- 若 Quarkus mq 中出现 producer、consumer、message builder、retry、transaction 等核心逻辑，应迁回 `mq-api` 或 `mq-pulsar`。
- Pulsar 连接参数校验和资源管理若不依赖 Quarkus，应保留在 `mq-pulsar`。

## 依赖边界

- `mq-api` 不得依赖 Pulsar、Quarkus、CDI。
- `mq-pulsar` 可以依赖 Apache Pulsar Client，但不得依赖 Quarkus。
- Quarkus mq 可以依赖 `mq-api`、`mq-pulsar`、Quarkus Arc、SmallRye Config 和 Quarkus lifecycle。

## 拆分任务清单

- 审查 Quarkus mq 是否有重复的 Pulsar core 逻辑。
- 将可复用配置校验和资源管理下沉到 `mq-pulsar`。
- 保持 adapter 只做配置读取、Bean 暴露和生命周期管理。
- 为 config mapping 到 Pulsar config 的转换增加测试。

## 验收标准

- `./gradlew :infrastructure-component-mq-api:test` 通过。
- `./gradlew :infrastructure-component-mq-pulsar:test` 通过。
- `./gradlew :infrastructure-component-quarkus:mq:test` 通过。
- `mq-api` 与 `mq-pulsar` 源码中没有 Quarkus、CDI、MicroProfile Config import。

## 模块审查补充

**解决的问题**：把独立 MQ API/Pulsar 实现装配进 Quarkus 配置和生命周期，解决应用内 producer、consumer、Pulsar client 的创建和关闭问题。

**如何使用**：依赖 `infrastructure-component-quarkus:mq`，在配置中使用 `msg.*` 前缀，例如 `msg.client.service-url`、`msg.producer.*`、`msg.consumer.*`、`msg.transaction.*`、`msg.monitoring.*`、`msg.retry.*`。业务侧通过 `MQClient` 创建 producer/consumer。

**当前依赖**：`implementation(libs.bundles.share)`、`api(project(":infrastructure-component-mq-api"))`、`api(project(":infrastructure-component-mq-pulsar"))`。

**需要注意**：Quarkus mq 只能做配置映射、producer/client 暴露和 lifecycle 关闭。Pulsar 连接、producer/consumer 实现、资源管理和配置校验应留在 `mq-pulsar`。审查时重点找是否有 Pulsar core 逻辑被复制到 adapter。
