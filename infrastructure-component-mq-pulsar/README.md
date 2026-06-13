# infrastructure-component-mq-pulsar

> `infrastructure-component-mq-api` SPI 的 Apache Pulsar 实现。通过 `MQClientBuilder` 创建 `MQClient`，业务代码只面向 SPI 接口编程，不直接依赖任何 Pulsar 原生 API。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-mq-pulsar"))
    // mq-api 已通过 api(...) 传递引入，无需额外声明
}
```

`build.gradle.kts` 中声明的是 `api(project(":infrastructure-component-mq-api"))`，因此引入本模块后 `mq-api` 的接口会自动透传到调用方 classpath。

---

## 核心实现类

| mq-api 接口 | Pulsar 实现类 | 说明 |
|---|---|---|
| `MQConfig` | `PulsarConfig` | 全量配置对象，内含六个嵌套配置块 |
| `MQClient` | `PulsarMQClient`（package-private） | 客户端，由 `MQClientBuilder` 创建 |
| `IProducerBuilder<T>` | `PulsarProducerBuilder<T>`（package-private） | 生产者构建器，流式 API |
| `IProducer<T>` | `PulsarProducer<T>` | 同步 / 异步发送消息 |
| `IConsumerBuilder<T>` | `PulsarConsumerBuilder<T>`（package-private） | 消费者构建器，流式 API |
| `IConsumer<T>` | `PulsarConsumer<T>`（record，package-private） | 消息接收与 Ack |
| `MessageBuilder<T>` / `Message<T>` | `PulsarMessageBuilder<T>` / `PulsarMessageBuilder.PulsarMessage<T>` | 消息封装层 |

入口工厂：

```
MQClientBuilder.createClient(MQConfig)
MQClientBuilder.createClient(MQConfig, OpenTelemetry)
```

已创建的 `MQClient` 实例由 `ResourcesManager` 以名称为键管理（`config.name()` 有值时用配置名，否则随机 UUID）。

---

## 配置

### 构建 `PulsarConfig` 的快捷构造方法

```java
// 仅指定服务地址（其余配置取默认值）
MQConfig config = new PulsarConfig("pulsar://localhost:6650");

// 指定地址 + JWT Token 认证
MQConfig config = new PulsarConfig("pulsar://localhost:6650", "eyJhbGc...");

// 指定地址 + 自定义认证插件
MQConfig config = new PulsarConfig("pulsar://localhost:6650",
        "org.apache.pulsar.client.impl.auth.AuthenticationToken",
        "token:eyJhbGc...");
```

### 配置块说明与默认值

#### `PulsarClientConfig`（连接层）

| 字段 | 默认值 | 说明 |
|---|---|---|
| `serviceUrl` | `pulsar://localhost:6650` | Broker 地址；TLS 使用 `pulsar+ssl://` 前缀 |
| `authToken` | `null` | JWT Token 认证（优先于 authPlugin） |
| `authPluginClassName` | `null` | 自定义认证插件全限定类名 |
| `authParams` | `null` | 自定义认证插件参数 |
| `operationTimeout` | `30s` | 操作超时 |
| `connectionTimeout` | `10s` | 连接超时 |
| `connectionsPerBroker` | `1` | 每个 Broker 的连接数 |
| `tcpNoDelay` | `true` | 禁用 Nagle 算法 |
| `keepAliveInterval` | `30s` | 心跳间隔 |
| `memoryLimit` | `67108864`（64MB） | 客户端内存上限（字节） |
| `maxLookupRequests` | `50000` | 最大 Lookup 请求数 |
| `maxLookupRedirects` | `20` | 最大 Lookup 重定向次数 |
| `maxConcurrentLookupRequests` | `5000` | 最大并发 Lookup 请求数 |

**TLS 子配置（`PulsarTlsConfig`）**

| 字段 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `false` | 是否启用 TLS（serviceUrl 以 `pulsar+ssl://` 开头时自动应用） |
| `trustCertsFilePath` | `null` | CA 证书路径 |
| `allowInsecureConnection` | `false` | 是否允许不安全连接 |
| `enableHostnameVerification` | `true` | 是否验证主机名 |

#### `PulsarProducerConfig`（生产者层）

| 字段 | 默认值 | 说明 |
|---|---|---|
| `sendTimeout` | `30s` | 发送超时 |
| `batchingEnabled` | `true` | 是否启用批量发送 |
| `batchingMaxMessages` | `1000` | 批次最大消息数 |
| `batchingMaxPublishDelay` | `1ms` | 批次最大等待时间 |
| `batchingMaxBytes` | `131072`（128KB） | 批次最大字节数 |
| `maxPendingMessages` | `1000` | 待发队列最大深度 |
| `blockIfQueueFull` | `"true"` | 队列满时是否阻塞 |
| `compressionType` | `LZ4` | 压缩算法（`NONE` / `LZ4` / `ZLIB` / `ZSTD` / `SNAPPY`） |
| `chunkingEnabled` | `false` | 是否启用消息分片 |
| `chunkMaxMessageSize` | `5242880`（5MB） | 分片最大字节数 |

#### `PulsarConsumerConfig`（消费者层）

| 字段 | 默认值 | 说明 |
|---|---|---|
| `ackTimeout` | `Duration.ZERO`（禁用） | Ack 超时（0 表示不超时） |
| `ackTimeoutTickTime` | `1s` | Ack 超时检测粒度 |
| `negativeAckRedeliveryDelay` | `1min` | nack 后重投延迟 |
| `receiverQueueSize` | `1000` | 接收队列大小 |
| `maxRedeliverCount` | `3` | 最大重投次数 |
| `deadLetterTopicSuffix` | `"-DLQ"` | 死信 Topic 后缀 |
| `retryTopicSuffix` | `"-RETRY"` | 重试 Topic 后缀 |
| `batchReceiveEnabled` | `false` | 是否启用批量接收 |
| `batchReceiveMaxMessages` | `100` | 批量接收最大消息数 |
| `batchReceiveTimeout` | `100ms` | 批量接收等待超时 |
| `subscriptionInitialPosition` | `Latest` | 订阅初始位置（`Latest` / `Earliest`） |
| `subscriptionType` | `EXCLUSIVE` | 订阅类型（`EXCLUSIVE` / `SHARED` / `FAILOVER` / `KEY_SHARED`） |
| `priority` | `0` | 消费者优先级 |
| `readCompacted` | `false` | 是否读压缩 Topic |
| `autoAck` | `false` | 是否在 MessageListener 中自动 Ack |

#### `PulsarTransactionConfig`（事务层）

| 字段 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `false` | 是否启用事务支持 |
| `coordinatorTopic` | `persistent://public/default/transaction-coordinator` | 事务协调 Topic |
| `timeout` | `1min` | 事务超时 |

#### `PulsarMonitoringConfig`（监控层）

| 字段 | 默认值 | 说明 |
|---|---|---|
| `metricsEnabled` | `true` | 是否启用指标采集（通过 OpenTelemetry） |
| `statsInterval` | `60s` | 统计周期 |
| `topicLevelMetricsEnabled` | `true` | Topic 级指标 |
| `consumerLevelMetricsEnabled` | `true` | 消费者级指标 |
| `producerLevelMetricsEnabled` | `true` | 生产者级指标 |

#### `PulsarRetryConfig`（重试层）

| 字段 | 默认值 | 说明 |
|---|---|---|
| `maxAttempts` | `3` | 最大重试次数 |
| `initialDelay` | `100ms` | 初始重试延迟 |
| `maxDelay` | `10s` | 最大重试延迟 |
| `multiplier` | `2.0` | 指数退避倍数 |

---

## 使用示例

### 1. 创建客户端

```java
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.pulsar.builder.MQClientBuilder;
import org.carl.infrastructure.mq.pulsar.config.PulsarConfig;

MQConfig config = new PulsarConfig("pulsar://localhost:6650");
MQClient client = MQClientBuilder.createClient(config);
```

### 2. 发送消息（泛型对象，AVRO Schema）

```java
import org.carl.infrastructure.mq.producer.IProducer;

// 使用 AVRO Schema 自动序列化 POJO
IProducer<MyOrder> producer = client.newProducer(MyOrder.class)
        .create("persistent://public/default/orders");

// 同步发送
IProducer.SendResult<MyOrder> result = producer.sendMessage(new MyOrder("id-1", 99.0));
System.out.println("messageId: " + result.getMessage().getMessageId());

// 带消息属性的同步发送
producer.sendMessage(new MyOrder("id-2", 199.0), msg -> msg
        .key("partition-key-1")
        .property("source", "checkout-service")
        .eventTime(System.currentTimeMillis()));

// 异步发送
producer.sendMessageAsync(new MyOrder("id-3", 299.0))
        .thenAccept(r -> System.out.println("sent: " + r.getMessage().getMessageId()));

producer.close();
```

### 3. 消费消息（MessageListener 推模式）

```java
import org.carl.infrastructure.mq.consumer.IConsumer;
import org.carl.infrastructure.mq.common.ex.ConsumerException;

IConsumer<MyOrder> consumer = client.newConsumer(MyOrder.class)
        .subscriptionName("order-processor")
        .autoAck(false)                          // 关闭自动 Ack，手动控制
        .messageListener((c, msg) -> {
            try {
                System.out.println("received: " + msg.getValue());
                c.acknowledge(msg);
            } catch (ConsumerException e) {
                c.negativeAcknowledge(msg);
            }
        })
        .subscribe("persistent://public/default/orders");

// consumer 持有监听线程，业务结束后关闭
consumer.close();
```

### 4. 消费消息（同步拉模式）

```java
import java.util.concurrent.TimeUnit;
import org.carl.infrastructure.mq.model.Message;

IConsumer<MyOrder> consumer = client.newConsumer(MyOrder.class)
        .subscriptionName("order-batch")
        .subscribe("persistent://public/default/orders");

Message<MyOrder> msg = consumer.receive(5, TimeUnit.SECONDS);
if (msg != null) {
    System.out.println("value: " + msg.getValue());
    consumer.acknowledge(msg);
}

consumer.close();
```

### 5. 关闭客户端

```java
// 释放所有 Producer/Consumer 后关闭 Client
client.close();

// 或一次性关闭由 ResourcesManager 管理的所有客户端
import org.carl.infrastructure.mq.pulsar.config.ResourcesManager;
ResourcesManager.closeAll();
```

---

## 与 SPI 的关系

本模块是 [`infrastructure-component-mq-api`](../infrastructure-component-mq-api) 所定义 SPI 的 Pulsar 实现。业务代码应**只 import mq-api 包下的类型**（`MQClient`、`IProducer`、`IConsumer`、`MQConfig` 等），实现类均为 `package-private`，不对外暴露。

SPI 核心接口位于：

```
org.carl.infrastructure.mq.client.MQClient
org.carl.infrastructure.mq.producer.IProducer<T>
org.carl.infrastructure.mq.producer.IProducerBuilder<T>
org.carl.infrastructure.mq.consumer.IConsumer<T>
org.carl.infrastructure.mq.consumer.IConsumerBuilder<T>
org.carl.infrastructure.mq.config.MQConfig
org.carl.infrastructure.mq.model.Message<T>
org.carl.infrastructure.mq.model.MessageBuilder<T>
```

---

## 注意事项

- **Schema 策略**：`newProducer(Class<T>)` / `newConsumer(Class<T>)` 使用 `Schema.AVRO(clazz)`，POJO 需满足 Avro 序列化要求（无参构造 / 标准 getter）；原始 `byte[]` 变体使用 `Schema.AUTO_PRODUCE_BYTES()`。
- **线程安全**：`PulsarMQClient` 和 `PulsarConsumer` 均标注了 `@NotThreadSafe`，不要在多线程间共享同一实例。
- **autoAck 行为**：`IConsumerBuilder.autoAck(true)` 会在 `MessageListener.received` 正常返回后自动 Ack，异常时自动 nack；默认为 `false`，需在 Listener 内手动调用 `consumer.acknowledge(msg)` / `consumer.negativeAcknowledge(msg)`。
- **TLS**：serviceUrl 改为 `pulsar+ssl://` 前缀后，TLS 参数（`allowInsecureConnection`、`enableHostnameVerification`、`trustCertsFilePath`）才生效；`PulsarTlsConfig.enabled` 字段本身不触发 TLS，协议前缀才是判断依据（见 `PulsarClientFactory`）。
- **配置校验**：可在创建客户端前调用 `PulsarConfigValidator.validate(config)` 对配置进行预检，违规配置会打印 warning/error 并在有硬错误时抛 `IllegalArgumentException`。
- **资源管理**：`ResourcesManager` 以名称为键维护所有已建 `MQClient`，可通过 `ResourcesManager.get(name)` 复用已有实例，`ResourcesManager.remove(name)` 会同时关闭对应 Client。
