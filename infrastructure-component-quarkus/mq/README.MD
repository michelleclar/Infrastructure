# infrastructure-component-quarkus / mq

把 Kafka 客户端（`infrastructure-component-mq-kafka`）接入 Quarkus CDI 应用。
应用启动时从 SmallRye Config 读取 `msg.*` 配置并创建全局 `MQClient` Bean，应用关闭时自动释放生产者、消费者和 Reader。

## 依赖

```kotlin
dependencies {
    implementation(project(":infrastructure-component-quarkus:mq"))
}
```

本模块传递引入 `infrastructure-component-mq-api` 和 `infrastructure-component-mq-kafka`，并引入 Quarkus Kafka Client 扩展以提供 native-image 所需的 Kafka 元数据。

## CDI Bean

| Bean | Scope | 来源 |
|------|-------|------|
| `MQClient` | `@ApplicationScoped` | `KafkaClientProvider` |
| `MsgArgsConfig` | Config Mapping | `@ConfigMapping(prefix = "msg")` |

`KafkaLifecycle` 在 Quarkus 关闭时调用 `MQClient.close()`。

## 最小配置

```properties
msg.client.service-url=localhost:9092
```

配置键继续使用公共 `MQConfig` 定义的 `msg.client.service-url`，其值会映射为 Kafka `bootstrap.servers`。

当前 Kafka 实现读取的主要配置如下：

- Producer：`send-timeout`、批量大小与延迟、压缩类型。
- Consumer：`subscription-initial-position`；`subscriptionName` 映射为 Kafka `group.id`。
- Reader：topic、起始位置和 reader name。

公共配置接口中保留的认证、TLS、监控、重试和事务字段尚未映射为 Kafka client properties，不应依赖这些字段配置 Kafka 安全连接或事务。

## 使用

```java
@ApplicationScoped
public class OrderService {
    @Inject
    MQClient mqClient;

    public void publish(Order order) throws ProducerException {
        try (IProducer<Order> producer = mqClient.newProducer(Order.class).create("orders")) {
            producer.sendMessage(order);
        }
    }
}
```

消费者必须显式设置 `subscriptionName`：

```java
IConsumer<Order> consumer = mqClient.newConsumer(Order.class)
        .subscriptionName("order-processor")
        .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
        .subscribe("orders");
```

`MQClient` 是应用级单例，不要在业务请求中关闭，由 `KafkaLifecycle` 在应用停止时统一释放。
