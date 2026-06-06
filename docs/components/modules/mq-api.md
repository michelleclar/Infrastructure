# mq-api

## 模块定位

`infrastructure-component-mq-api` 是消息队列抽象层，定义 client、producer、consumer、message、processor、transaction 和异常模型，不绑定具体 MQ 产品。

## 核心能力

- `MQClient` 和 `IClientBuilder`。
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

- 业务代码只依赖 MQ 抽象，不关心底层 Pulsar 实现。
- 测试中用 fake producer/consumer 替换真实 MQ。
- Quarkus mq adapter 根据配置创建具体 MQ client。

## 维护事项

- `MQClient.builder()` 当前返回 `null`，后续应明确是保留 SPI 入口还是删除。
- API 层新增能力时要确认 Pulsar 实现能落地，避免抽象空转。
- 异常类型应稳定，减少实现层异常向业务泄漏。

## 测试验收

- `./gradlew :infrastructure-component-mq-api:test` 通过。
- mq-api 源码中没有 Pulsar、Quarkus、CDI import。
- producer/consumer/message builder 的契约有单元测试或接口兼容测试。

## 使用与依赖补充

**为了解决什么**：让业务只面向 MQ 抽象编程，不直接绑定 Pulsar、Kafka 或 Quarkus。

**如何使用**：业务代码接收 `MQClient`，通过 `newProducer()` 或 `newConsumer()` 创建 producer/consumer，再用 `IProducer.sendMessage(...)` 发送消息或用 consumer listener 处理消息。具体 client 由 `mq-pulsar` 或 Quarkus mq adapter 创建。

**当前依赖了什么**：无生产依赖，测试只依赖 JUnit。

**需要注意什么**：`MQClient.builder()` 当前返回 `null`，审查时要决定删除、实现 SPI，或改由具体实现 builder 提供。API 层不要引入 Pulsar 专有枚举和异常。
