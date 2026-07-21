# mq-api

## 模块定位

`infra-component-mq-api` 是消息队列抽象层，定义 client、producer、consumer、message、processor、transaction 和异常模型，不绑定具体 MQ 产品。

## 核心能力

- `MQClient`、`MQClientFactory` 和 `MQClientProvider`。
- `IProducer`、`IProducerBuilder`、producer stats 与发送结果。
- `IConsumer`、`IConsumerBuilder`、message listener、consumer stats。
- `Message`、`MessageBuilder`。
- subscription、compression、routing、hashing 等通用枚举。
- `MQConfig` 和 MQ 异常体系。

## 依赖边界

- 不依赖 Pulsar、Kafka、RabbitMQ 或 Quarkus。
- 不依赖 CDI、JAX-RS、MicroProfile Config。
- 具体实现模块依赖 mq-api，mq-api 不反向依赖实现模块。

## 对外 API

- `MQClient#newProducer()`。
- `MQClient#newConsumer()`。
- `IProducer#sendMessage(...)`。
- `IConsumer` 的订阅、接收和关闭接口。
- `ProcessorBuilder` 用于组装消息处理流程。

## 典型使用场景

- 业务代码只依赖 MQ 抽象，不关心底层 Kafka/Pulsar 实现。
- 测试中用 fake producer/consumer 替换真实 MQ。
- Quarkus mq adapter 根据配置创建具体 MQ client。

## 维护事项

- `MQClient.builder()` 是已废弃的历史入口；统一使用 `MQClientFactory.create(MQConfig)`。
- API 层新增能力时要确认 Kafka 和 Pulsar 都能落地，无法等价映射时必须明确能力边界。
- 异常类型应稳定，减少实现层异常向业务泄漏。

## 测试验收

- `./gradlew :infra-component-mq-api:test` 通过。
- mq-api 源码中没有 Pulsar、Quarkus、CDI import。
- producer/consumer/message builder 的契约有单元测试或接口兼容测试。

## 使用与依赖补充

**为了解决什么**：让业务只面向 MQ 抽象编程，不直接绑定 Pulsar、Kafka 或 Quarkus。

**如何使用**：业务代码接收 `MQClient`；普通 Java/Spring 装配层调用 `MQClientFactory.create(config)`。运行时二选一引入 `mq-kafka` 或 `mq-pulsar`；Quarkus 应用二选一引入对应 Quarkus Provider 集成模块。

**当前依赖了什么**：无生产依赖，测试只依赖 JUnit。

**需要注意什么**：运行时没有 Provider 或同时存在多个 Provider 都会明确失败。API 层不要继续加入具体产品专属枚举和配置。
