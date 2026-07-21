# mq-kafka

## 模块定位

`infra-component-mq-kafka` 是 `mq-api` 的 Apache Kafka 实现，通过 Java SPI 注册 Kafka Provider。

## 核心能力

- `KafkaMQClientProvider` 和兼容入口 `MQClientBuilder`。
- Kafka producer、consumer、reader 及其 builder。
- `KafkaConfig` 与配置校验。

## 依赖边界

- 依赖 `infra-component-mq-api` 和 Apache Kafka Client。
- 不依赖 Quarkus、CDI、SmallRye Config 或 MicroProfile Config。

## 使用

普通 Java/Spring 装配层调用 `MQClientFactory.create(config)`；Quarkus 应用引入 `infra-component-quarkus:mq-kafka` 并注入 `MQClient`。运行时不得同时引入 Pulsar Provider。

公共配置和 builder 中无法映射为 Kafka 等价行为的能力必须明确报告或记录，不得据字段存在推断功能已生效。
