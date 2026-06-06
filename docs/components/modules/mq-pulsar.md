# mq-pulsar

## 模块定位

`infrastructure-component-mq-pulsar` 是 `mq-api` 的 Apache Pulsar 实现，负责把通用 producer/consumer/client 抽象映射到 Pulsar Client。

## 核心能力

- `PulsarMQClient`：`MQClient` 实现。
- `MQClientBuilder`、`PulsarClientFactory`。
- `PulsarProducer`、`PulsarProducerBuilder`。
- `PulsarConsumer`、`PulsarConsumerBuilder`。
- `PulsarMessageBuilder`。
- `PulsarConfig`、`PulsarConfigValidator`、`ResourcesManager`。
- `TimeLimitedConsumerEventListener`。

## 依赖边界

- 可以依赖 `org.apache.pulsar:pulsar-client`。
- 依赖 `infrastructure-component-mq-api`。
- 不依赖 Quarkus、CDI、SmallRye Config 或 MicroProfile Config。
- Quarkus mq adapter 负责配置读取和 Bean 生命周期，不放在本模块。

## 对外 API

- `MQClientBuilder` 创建 Pulsar MQ client。
- producer/consumer builder 兼容 mq-api。
- `PulsarConfig` 描述 Pulsar 连接、producer、consumer、TLS、transaction 等配置。

## 典型使用场景

- 非 Quarkus Java 应用直接使用 Pulsar MQ 能力。
- Quarkus mq adapter 使用本模块创建 Pulsar client。
- 集成测试中验证 Pulsar producer/consumer 行为。

## 维护事项

- Pulsar Client 版本升级要验证 producer、consumer、schema、transaction 兼容。
- 资源关闭逻辑必须幂等，避免 Quarkus lifecycle 和业务手动关闭冲突。
- 不要把 `quarkus.plugins.*` 配置前缀写入本模块。

## 测试验收

- `./gradlew :infrastructure-component-mq-pulsar:test` 通过。
- mq-pulsar 源码中没有 Quarkus、CDI、MicroProfile Config import。
- 配置校验、client 创建、producer/consumer builder 有测试覆盖。

## 使用与依赖补充

**为了解决什么**：把 `mq-api` 的 producer、consumer、client 抽象落到 Apache Pulsar，实现消息收发、订阅、批量、压缩、事务等 Pulsar 能力。

**如何使用**：直接使用时构造 `PulsarConfig`，通过 `MQClientBuilder` 或 `PulsarClientFactory` 创建 `MQClient`。Quarkus 应用通常不直接使用本模块 builder，而是通过 `infrastructure-component-quarkus:mq` 的 `msg.*` 配置创建。

**当前依赖了什么**：`org.apache.pulsar:pulsar-client:4.1.2`，并 `api(project(":infrastructure-component-mq-api"))`。

**需要注意什么**：不要读取 Quarkus 配置，不要加 CDI 注解。审查时重点看 resource close 是否幂等、Pulsar 异常是否被转换为 mq-api 异常、泛型消息序列化是否稳定。
