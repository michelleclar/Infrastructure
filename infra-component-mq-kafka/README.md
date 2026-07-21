# infra-component-mq-kafka

`infra-component-mq-api` SPI 的 Apache Kafka 实现。模块通过 Java SPI 注册 `kafka` Provider；普通 Java/Spring 应用使用公共工厂创建客户端，不需要 import Kafka Builder。

## 依赖与创建

```kotlin
dependencies {
    runtimeOnly(project(":infra-component-mq-kafka"))
    implementation(project(":infra-component-mq-api"))
}
```

```java
import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.client.MQClientFactory;
import org.carl.infra.mq.config.MQConfig;

MQConfig config = ...; // 由应用配置层提供
MQClient client = MQClientFactory.create(config);
```

`org.carl.infra.mq.kafka.builder.MQClientBuilder` 作为兼容入口保留。运行时 classpath 中只能存在一个 MQ Provider；不得同时引入 Kafka 和 Pulsar 实现。

## 语义边界

Kafka Provider 实现公共 producer、consumer、reader 和 client 接口。公共接口中带有 Pulsar 产品语义的能力无法保证等价映射；认证、TLS、监控、重试和事务等配置必须以 Kafka Provider 实际支持范围为准，不能根据字段存在推断其已经生效。

公共负载均衡订阅使用：

```java
client.newConsumer(MyOrder.class)
        .subscriptionName("orders")
        .subscriptionType(SubscriptionTypes.LOAD_BALANCED)
        .subscribe("orders");
```

Kafka 收到不支持的能力对象或 Provider option 时会立即抛出
`UnsupportedMQCapabilityException`，不会记录日志后继续运行。
