# mq

## 当前定位

MQ 被拆分为产品中立 API、Kafka/Pulsar 独立实现、Quarkus 公共装配层和两个 Provider 集成模块。

## 模块边界

- `infra-component-mq-api`：公共接口、`MQClientFactory` 和 `MQClientProvider` SPI。
- `infra-component-mq-kafka`：Kafka 实现及 Java SPI 注册，不依赖 Quarkus。
- `infra-component-mq-pulsar`：Pulsar 实现及 Java SPI 注册，不依赖 Quarkus。
- `infra-component-quarkus:mq`：`msg.*` 配置映射和通用生命周期，不依赖具体 MQ SDK。
- `infra-component-quarkus:mq-kafka`：Kafka `MQClient` CDI producer 和 Quarkus Kafka 扩展。
- `infra-component-quarkus:mq-pulsar`：Pulsar `MQClient` CDI producer 和 Quarkus Pulsar 扩展。

## 使用

Quarkus 应用在 `mq-kafka` 与 `mq-pulsar` 集成模块中二选一，并配置必填的 `msg.client.service-url`。业务只注入 `MQClient`。Provider 缺失或两个 Provider 集成模块同时出现时，应用启动会明确失败。

普通 Java/Spring 应用只在运行时放置一个实现模块，由 `MQClientFactory` 发现并创建客户端；没有 Provider 或存在多个 Provider 时明确失败。

## 验收

- `./gradlew :infra-component-mq-api:test`。
- `./gradlew :infra-component-mq-kafka:test`。
- `./gradlew :infra-component-mq-pulsar:test`。
- `./gradlew :infra-component-quarkus:mq-kafka:test`。
- `./gradlew :infra-component-quarkus:mq-pulsar:test`。
- 三个独立 MQ 模块源码中没有 Quarkus、CDI、MicroProfile Config import。
