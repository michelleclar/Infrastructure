# infrastructure-component-quarkus / mq

把 Pulsar MQ 客户端（`infrastructure-component-mq-pulsar`）接入 Quarkus CDI 应用。
启动时自动从配置系统读取连接参数、构建 `MQClient` bean，并在应用关闭时优雅地释放连接——业务代码只需注入 `MQClient`，通过其 builder API 创建生产者或消费者。

> MQ API 的完整类型（`IProducer` / `IConsumer` / `IProducerBuilder` / `IConsumerBuilder` / `MessageListener` 等）定义在
> [`infrastructure-component-mq-api`](../../infrastructure-component-mq-api)。
> Pulsar 实现细节在 [`infrastructure-component-mq-pulsar`](../../infrastructure-component-mq-pulsar)。
> 本模块只讲 **CDI 接线**。

---

## 依赖

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:mq"))
}
```

本模块通过 `api` 传递引入 `infrastructure-component-mq-api` 和 `infrastructure-component-mq-pulsar`，
业务侧可以直接使用 `MQClient`、`IProducer`、`IConsumer` 等类型，无需额外声明依赖。

---

## CDI 产出的 Bean

| Bean 类型 | Scope | 产出来源 | 说明 |
|-----------|-------|---------|------|
| `MQClient` | `@ApplicationScoped` | `PulsarClientProvide` | 全局唯一客户端，注入即用 |
| `MsgArgsConfig` | — | SmallRye Config | `@ConfigMapping` 接口，可直接注入读取配置 |

`PulsarLifecycle`（`@ApplicationScoped`）在 `StartupEvent` 时记录日志，在 `ShutdownEvent` 时调用 `mqClient.close()` 关闭连接。

---

## 配置（前缀 `msg`）

所有配置项均来自 `MsgArgsConfig`（`@ConfigMapping(prefix = "msg")`）。

### 客户端（`msg.client.*`）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `msg.client.service-url` | `pulsar://localhost:6650` | Pulsar broker 地址 |
| `msg.client.auth-plugin-class-name` | — | 认证插件类名（可选） |
| `msg.client.auth-params` | — | 认证参数字符串（可选） |
| `msg.client.auth-token` | — | Token 认证（可选） |
| `msg.client.operation-timeout` | `30s` | 操作超时 |
| `msg.client.connection-timeout` | `10s` | 连接超时 |
| `msg.client.connections-per-broker` | `1` | 每个 broker 的连接数 |
| `msg.client.tcp-no-delay` | `true` | 禁用 Nagle 算法 |
| `msg.client.keep-alive-interval` | `30s` | 心跳保活间隔 |
| `msg.client.memory-limit` | `67108864`（64 MB） | 客户端内存限制（字节） |
| `msg.client.max-lookup-requests` | `50000` | 最大 lookup 请求数 |
| `msg.client.max-lookup-redirects` | `20` | 最大 lookup 重定向次数 |
| `msg.client.max-concurrent-lookup-requests` | `5000` | 最大并发 lookup 请求数 |
| `msg.client.tls.enabled` | `false` | 是否启用 TLS |
| `msg.client.tls.trust-certs-file-path` | — | CA 证书路径（可选） |
| `msg.client.tls.allow-insecure-connection` | `false` | 是否允许不安全连接 |
| `msg.client.tls.enable-hostname-verification` | `false` | 是否验证主机名 |

### 生产者默认值（`msg.producer.*`）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `msg.producer.send-timeout` | `30s` | 发送超时 |
| `msg.producer.batching-enabled` | `true` | 是否启用批量发送 |
| `msg.producer.batching-max-messages` | `1000` | 批量最大消息数 |
| `msg.producer.batching-max-publish-delay` | `1ms` | 批量最大等待延迟 |
| `msg.producer.batching-max-bytes` | `131072`（128 KB） | 批量最大字节数 |
| `msg.producer.max-pending-messages` | `1000` | 最大等待发送队列长度 |
| `msg.producer.block-if-queue-full` | `BLOCK` | 队列满时的行为 |
| `msg.producer.compression-type` | `none` | 压缩类型（`none` / `lz4` / `zlib` / `zstd` / `snappy`） |
| `msg.producer.chunking-enabled` | `false` | 是否启用消息分块 |
| `msg.producer.chunk-max-message-size` | `5242880`（5 MB） | 分块最大消息大小（字节） |

### 消费者默认值（`msg.consumer.*`）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `msg.consumer.auto-ack` | `true` | 是否自动确认 |
| `msg.consumer.ack-timeout` | `30s` | 未确认消息超时 |
| `msg.consumer.ack-timeout-tick-time` | `1s` | 超时检查粒度 |
| `msg.consumer.negative-ack-redelivery-delay` | `1m` | 否定确认后重投递延迟 |
| `msg.consumer.receiver-queue-size` | `1000` | 接收队列大小 |
| `msg.consumer.max-redeliver-count` | `3` | 最大重投递次数 |
| `msg.consumer.dead-letter-topic-suffix` | `-dlq` | 死信 topic 后缀 |
| `msg.consumer.retry-topic-suffix` | `-retry` | 重试 topic 后缀 |
| `msg.consumer.batch-receive-enabled` | `false` | 是否启用批量接收 |
| `msg.consumer.batch-receive-max-messages` | `100` | 批量接收最大消息数 |
| `msg.consumer.batch-receive-timeout` | `100ms` | 批量接收超时 |
| `msg.consumer.subscription-initial-position` | `LATEST` | 新订阅的起始消费位置（`LATEST` / `Earliest`） |
| `msg.consumer.priority` | `0` | 消费者优先级 |
| `msg.consumer.read-compacted` | `false` | 是否从压缩 topic 读取 |
| `msg.consumer.subscription-type` | `Exclusive` | 订阅类型（见下表） |

**订阅类型**（`SubscriptionType` 枚举）：

| 值 | 说明 |
|----|------|
| `EXCLUSIVE` | 独占：一个订阅只能有一个消费者 |
| `SHARED` | 共享：Round-Robin 分发给多个消费者 |
| `FAILOVER` | 故障转移：主消费者断开后自动切换到备用 |
| `KEY_SHARED` | 按 key 分区：同一 key 总由同一消费者处理 |

### 事务（`msg.transaction.*`）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `msg.transaction.enabled` | `false` | 是否启用事务 |
| `msg.transaction.coordinator-topic` | `persistent://pulsar/system/transaction_coordinator_assign` | 事务协调器 topic |
| `msg.transaction.timeout` | `60s` | 事务超时 |
| `msg.transaction.buffer-snapshot-segment-size` | `262144`（256 KB） | buffer 快照段大小 |
| `msg.transaction.buffer-snapshot-min-time-in-millis` | `5s` | buffer 快照最小时间间隔 |
| `msg.transaction.buffer-snapshot-max-transaction-count` | `1000` | buffer 快照最大事务数 |

### 监控（`msg.monitoring.*`）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `msg.monitoring.metrics-enabled` | `true` | 是否启用指标 |
| `msg.monitoring.stats-interval` | `60s` | 统计上报间隔 |
| `msg.monitoring.topic-level-metrics-enabled` | `true` | 是否启用 topic 级别指标 |
| `msg.monitoring.consumer-level-metrics-enabled` | `true` | 是否启用消费者级别指标 |
| `msg.monitoring.producer-level-metrics-enabled` | `true` | 是否启用生产者级别指标 |

### 重试（`msg.retry.*`）

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `msg.retry.max-attempts` | `3` | 最大重试次数 |
| `msg.retry.initial-delay` | `1s` | 初始重试延迟 |
| `msg.retry.max-delay` | `30s` | 最大重试延迟 |
| `msg.retry.multiplier` | `2.0` | 退避系数 |
| `msg.retry.retryable-exceptions` | — | 可重试异常类名列表（可选） |

---

## CDI 用法

注入 `MQClient`，通过 builder API 创建生产者或消费者：

```java
@ApplicationScoped
public class MyService {

    @Inject
    MQClient mqClient;

    // ...
}
```

`MQClient` 的核心方法：

| 方法 | 说明 |
|------|------|
| `newProducer()` | 创建 `byte[]` 类型生产者 builder |
| `newProducer(Class<T> clazz)` | 创建类型化生产者 builder |
| `newConsumer()` | 创建 `byte[]` 类型消费者 builder |
| `newConsumer(Class<T> clazz)` | 创建类型化消费者 builder |

---

## 使用示例

### 生产者：发送消息

```java
@ApplicationScoped
public class OrderService {

    @Inject
    MQClient mqClient;

    public void publishOrder(Order order) throws ProducerException {
        IProducer<Order> producer = mqClient.newProducer(Order.class)
                .create("persistent://public/default/orders");

        // 同步发送
        IProducer.SendResult<Order> result = producer.sendMessage(order);

        // 异步发送
        producer.sendMessageAsync(order)
                .thenAccept(r -> log.info("sent: {}", r.getMessage().getMessageId()));
    }
}
```

### 消费者：监听器模式（推荐）

```java
@ApplicationScoped
public class OrderConsumer {

    @Inject
    MQClient mqClient;

    void onStart(@Observes StartupEvent ev) throws ConsumerException {
        mqClient.newConsumer(Order.class)
                .subscriptionName("order-processor")
                .messageListener((consumer, msg) -> {
                    try {
                        process(msg.getValue());
                        consumer.acknowledge(msg);
                    } catch (Exception e) {
                        consumer.negativeAcknowledge(msg);
                    }
                })
                .subscribe("persistent://public/default/orders");
    }

    private void process(Order order) {
        // 业务逻辑
    }
}
```

### 消费者：手动拉取模式

```java
@ApplicationScoped
public class BatchProcessor {

    @Inject
    MQClient mqClient;

    public void poll() throws ConsumerException {
        IConsumer<Order> consumer = mqClient.newConsumer(Order.class)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscriptionName("batch-processor")
                .subscribe("persistent://public/default/orders");

        Message<Order> msg = consumer.receive(5, TimeUnit.SECONDS);
        if (msg != null) {
            process(msg.getValue());
            consumer.acknowledge(msg);
        }
    }
}
```

### 典型 application.properties 最小配置

```properties
# 必填：broker 地址
msg.client.service-url=pulsar://localhost:6650

# 生产环境建议：TLS + Token
%prod.msg.client.service-url=pulsar+ssl://mq.example.com:6651
%prod.msg.client.auth-token=${MSG_AUTH_TOKEN}
%prod.msg.client.tls.enabled=true
%prod.msg.client.tls.trust-certs-file-path=/etc/ssl/certs/mq-ca.pem
%prod.msg.producer.compression-type=lz4
%prod.msg.consumer.max-redeliver-count=5
```

---

## 注意事项

- **`MQClient` 是 `@ApplicationScoped` 单例**，不要在请求处理中调用 `close()`，由 `PulsarLifecycle` 在应用关闭时统一释放。
- **`IProducer` / `IConsumer` 不是 CDI bean**，通过 `MQClient` builder 按需创建；若需跨请求复用，自行持有引用并在 `@PreDestroy` 中关闭。
- 消费者的 `subscriptionName` 必须显式指定，否则 builder 会报错。
- `subscription-type=Exclusive` 时同一订阅名只能有一个消费者进程；水平扩展时请改用 `SHARED` 或 `KEY_SHARED`。
- 事务功能默认关闭（`msg.transaction.enabled=false`），需要在 broker 侧同步开启事务协调器后才能使用。
