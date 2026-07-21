# infra-component-quarkus / mq

MQ 的 Quarkus 公共装配层，负责 `msg.*` 配置映射和 `MQClient` 生命周期管理，不依赖 Kafka 或 Pulsar 客户端。

应用不要单独依赖本模块，而是在下列 Provider 集成模块中二选一：

```kotlin
dependencies {
    // Kafka
    implementation(project(":infra-component-quarkus:mq-kafka"))

    // 或 Pulsar
    // implementation(project(":infra-component-quarkus:mq-pulsar"))
}
```

两个 Provider 集成模块都会传递引入本模块和 `infra-component-mq-api`。业务代码统一注入：

```java
@Inject
MQClient mqClient;
```

`msg.client.service-url` 是必填配置，不提供产品默认值：

```properties
# Kafka Provider
msg.client.service-url=localhost:9092

# Pulsar Provider
# msg.client.service-url=pulsar://localhost:6650
```

Kafka 与 Pulsar 集成模块不能同时引入；缺失 Provider 或产生多个 `MQClient` Bean 时，应用启动会明确失败。切换 Provider 时只替换依赖、调整配置并运行对应集成测试；业务生产和消费代码不变。
