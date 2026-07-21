# infra-component-quarkus / mq-pulsar

Pulsar Provider 的 Quarkus 集成模块。传递引入公共 MQ 装配层、`infra-component-mq-pulsar` 和 Quarkus Pulsar 扩展。

```kotlin
dependencies {
    implementation(project(":infra-component-quarkus:mq-pulsar"))
}
```

```properties
msg.client.service-url=pulsar://localhost:6650
```

模块生产应用级 `MQClient` Bean，应用关闭时由公共装配层统一释放。不要同时引入 `infra-component-quarkus:mq-kafka`。
