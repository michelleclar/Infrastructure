# infra-component-quarkus / mq-kafka

Kafka Provider 的 Quarkus 集成模块。传递引入公共 MQ 装配层、`infra-component-mq-kafka` 和 Quarkus Kafka Client 扩展。

```kotlin
dependencies {
    implementation(project(":infra-component-quarkus:mq-kafka"))
}
```

```properties
msg.client.service-url=localhost:9092
```

模块生产应用级 `MQClient` Bean，应用关闭时由公共装配层统一释放。不要同时引入 `infra-component-quarkus:mq-pulsar`。
