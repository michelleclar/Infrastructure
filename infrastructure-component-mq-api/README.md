# infrastructure-component-mq-api

消息队列抽象层（SPI）。定义生产者、消费者、消息模型和配置契约的纯接口，业务代码面向这套接口编程，运行时行为由具体实现模块注入。当前官方实现：[infrastructure-component-mq-pulsar](../infrastructure-component-mq-pulsar)。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-mq-api"))
}
```

模块本身无第三方运行时依赖（测试依赖 JUnit 5）。

---

## 模块结构

```
org.carl.infrastructure.mq
├── client/        MQClient（客户端门面）、IClientBuilder
├── model/         Message<T>、MessageBuilder<T>
├── producer/      IProducer<T>、IProducerBuilder<T>、枚举
├── consumer/      IConsumer<T>、IConsumerBuilder<T>、MessageListener<T>、枚举
├── config/        MQConfig 及各子配置接口
├── tx/            ITransactional
├── processor/     ProcessorBuilder（内部组合用，早期）
└── common/ex/     MQClientException、ProducerException、ConsumerException
```

---

## 核心接口与类型

### 客户端

| 接口 / 类 | 职责 |
|---|---|
| `MQClient` | 顶层客户端，创建生产者与消费者；实现 `AutoCloseable` |
| `IClientBuilder` | 构建 `MQClient` 实例的 SPI 入口（实现由具体模块提供） |

`MQClient` 的核心方法：

| 方法签名 | 说明 |
|---|---|
| `IProducerBuilder<byte[]> newProducer()` | 创建字节数组类型的生产者构建器 |
| `<T> IProducerBuilder<T> newProducer(Class<T> clazz)` | 创建泛型类型的生产者构建器 |
| `IConsumerBuilder<byte[]> newConsumer()` | 创建字节数组类型的消费者构建器 |
| `<T> IConsumerBuilder<T> newConsumer(Class<T> clazz)` | 创建泛型类型的消费者构建器 |
| `void close() / CompletableFuture<Void> closeAsync()` | 关闭客户端 |

---

### 消息模型

| 接口 | 职责 |
|---|---|
| `Message<T>` | 不可变消息对象；提供 `getValue()`、`getKey()`、`getTopic()`、`getMessageId()`、`getProperties()`、`getEventTime()`、`getSequenceId()` 等读取方法 |
| `MessageBuilder<T>` | 链式消息构建器；支持 `value`、`key`、`property`、`eventTime`、`sequenceId`、`deliverAfter`（延迟投递）、`deliverAt`（定时投递）、`disableReplication`；调用 `build()` 生成 `Message<T>` |

---

### 生产者

| 接口 / 类型 | 职责 |
|---|---|
| `IProducerBuilder<T>` | 生产者构建器；通过 `create(String topicName)` 得到 `IProducer<T>` |
| `IProducer<T>` | 消息生产者，实现 `AutoCloseable` |
| `IProducer.SendResult<T>` | 发送结果；`isSuccess()`、`getMessage()`、`getErrorMessage()` |
| `IProducer.SendCallback<T>` | 单条发送异步回调：`onSuccess` / `onFailure` |
| `IProducer.BatchSendCallback<T>` | 批量发送回调：`onSuccess` / `onFailure` / `onPartialSuccess` |
| `IProducer.ProducerStats` | 运行时统计（@Deprecated，监控备用） |

`IProducer<T>` 核心方法分组：

| 分组 | 代表方法 |
|---|---|
| 同步发送 | `sendMessage(T value)` / `sendMessage(T value, Consumer<MessageBuilder<T>> consumer)` |
| 异步发送 | `sendMessageAsync(T value)` → `CompletableFuture<SendResult<T>>` |
| 批量发送 | `sendMessages(List<MessageBuilder<T>> messages)` / `sendMessagesAsync(...)` |
| 延迟发送 | `sendDelayedMessage(MessageBuilder<T> message, long delayMillis)` |
| 事务发送 | `sendMessageInTransaction(MessageBuilder<T> message)` / `sendMessageInTransactionAsync(...)` |
| 管理 | `flush()` / `flushAsync()` / `isConnected()` / `getProducerName()` |

`IProducerBuilder<T>` 常用配置项：

| 方法 | 说明 |
|---|---|
| `create(String topicName)` | 创建并连接生产者 |
| `conf(Consumer<MQConfig.ProducerConfig> config)` | 函数式配置（推荐） |
| `producerName(String)` | 设置生产者名称 |
| `accessMode(ProducerAccessMode)` | 访问模式：`Shared` / `Exclusive` / `ExclusiveWithFencing` / `WaitForExclusive` |
| `compressionType(CompressionType)` | 压缩：`NONE` / `LZ4` / `ZLIB` / `ZSTD` / `SNAPPY` |
| `messageRoutingMode(MessageRoutingMode)` | 分区路由：`SinglePartition` / `RoundRobinPartition` / `CustomPartition` |
| `hashingScheme(HashingScheme)` | 分区哈希：`JavaStringHash` / `Murmur3_32Hash` |
| `enableBatching(boolean)` / `batchingMaxMessages(int)` | 批量发送开关与上限 |
| `sendTimeout(int, TimeUnit)` | 发送超时 |

---

### 消费者

| 接口 / 类型 | 职责 |
|---|---|
| `IConsumerBuilder<T>` | 消费者构建器；通过 `subscribe(String... topic)` 得到 `IConsumer<T>` |
| `IConsumer<T>` | 消息消费者 |
| `MessageListener<T>` | 推送模式监听器；`received(IConsumer<T>, Message<T>)` 处理消息，`onException` 处理异常 |
| `ConsumerStats` | 消费者运行时统计（@Deprecated，监控备用） |

`IConsumer<T>` 核心方法：

| 分组 | 代表方法 |
|---|---|
| 接收 | `receive()` / `receive(int timeout, TimeUnit unit)` / `receiveAsync()` |
| 确认 | `acknowledge(Message<T>)` / `acknowledgeAsync(...)` |
| 累积确认 | `acknowledgeCumulative(Message<T>)` / `acknowledgeCumulativeAsync(...)` |
| 否定确认 | `negativeAcknowledge(Message<T>)`（触发重新投递） |
| 位移重置 | `seek(long timestamp)` / `seek(String messageId)` |
| 控制 | `pause()` / `resume()` / `close()` / `closeAsync()` |

`IConsumerBuilder<T>` 常用配置项：

| 方法 | 说明 |
|---|---|
| `subscribe(String... topic)` | 订阅并创建消费者 |
| `conf(Consumer<MQConfig.ConsumerConfig> config)` | 函数式配置（推荐） |
| `subscriptionName(String)` | 订阅名称（必填） |
| `subscriptionType(SubscriptionType)` | 订阅类型（见下表） |
| `subscriptionMode(SubscriptionMode)` | `Durable`（持久）/ `NonDurable` |
| `subscriptionInitialPosition(SubscriptionInitialPosition)` | `Latest` / `Earliest` |
| `messageListener(MessageListener<T>)` | 注册推送监听器 |
| `ackTimeout(long, TimeUnit)` | 未确认超时，超时后重新投递 |
| `autoAck(boolean)` | 是否自动确认 |
| `receiverQueueSize(int)` | 预取队列大小（默认 1000） |
| `enableRetry(boolean)` | 是否开启自动重试 |

`SubscriptionType` 枚举值：

| 值 | 含义 |
|---|---|
| `EXCLUSIVE` | 独占订阅，同时只有一个消费者 |
| `SHARED` | 共享订阅，多消费者轮询分发 |
| `FAILOVER` | 故障转移，一个活跃其余待机 |
| `KEY_SHARED` | 按消息 Key 固定分发到同一消费者 |

---

### 配置契约

`MQConfig` 是顶层配置接口，分为六个子接口：

| 子接口 | 职责 |
|---|---|
| `MQConfig.ClientConfig` | 服务地址、认证、超时、连接数、TLS 等客户端参数 |
| `MQConfig.ProducerConfig` | 发送超时、批量、压缩、分块、队列等生产者参数 |
| `MQConfig.ConsumerConfig` | 确认超时、重试、死信队列、批量接收、订阅位置等消费者参数 |
| `MQConfig.TransactionConfig` | 事务开关、协调器主题、超时、快照参数 |
| `MQConfig.MonitoringConfig` | 指标收集开关、统计间隔、Topic / Consumer / Producer 级统计开关 |
| `MQConfig.RetryConfig` | 最大重试次数、初始/最大延迟、退避倍数、可重试异常类型 |

---

### 事务与异常

| 类型 | 说明 |
|---|---|
| `ITransactional` | 事务 SPI：`commit()` / `rollback()` |
| `MQClientException` | 客户端级别异常（客户端创建/关闭） |
| `ProducerException` | 生产者操作异常 |
| `ConsumerException` | 消费者操作异常 |

---

## 使用示例

### 发送消息（同步）

```java
// 由具体实现（如 mq-pulsar）提供 MQClient 实例，此处假设已注入
MQClient client = ...; // 由 infrastructure-component-mq-pulsar 实现并注入

IProducer<String> producer = client.newProducer(String.class)
        .producerName("order-producer")
        .compressionType(CompressionType.LZ4)
        .create("persistent://public/default/order-topic");

IProducer.SendResult<String> result = producer.sendMessage(
        "order-payload",
        msg -> msg.key("order-123")
                  .property("region", "cn-east")
                  .eventTime(System.currentTimeMillis())
);

if (result.isSuccess()) {
    System.out.println("sent: " + result.getMessage().getMessageId());
}
producer.close();
```

### 发送消息（异步 + 回调）

```java
producer.sendMessageAsync("order-payload", msg -> msg.key("order-123"))
        .thenAccept(result -> System.out.println("async sent: " + result.getMessage().getMessageId()))
        .exceptionally(ex -> { ex.printStackTrace(); return null; });
```

### 延迟发送

```java
MessageBuilder<String> msg = ...; // 由实现模块创建，或通过 producer 获得
producer.sendDelayedMessage(msg, 5_000L); // 5 秒后投递
```

### 拉取消费（同步阻塞）

```java
IConsumer<String> consumer = client.newConsumer(String.class)
        .subscriptionName("order-sub")
        .subscriptionType(SubscriptionType.SHARED)
        .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
        .subscribe("persistent://public/default/order-topic");

while (true) {
    Message<String> msg = consumer.receive(5, TimeUnit.SECONDS);
    if (msg == null) continue;
    try {
        System.out.println("received: " + msg.getValue());
        consumer.acknowledge(msg);
    } catch (Exception e) {
        consumer.negativeAcknowledge(msg);
    }
}
```

### 推送消费（MessageListener）

```java
IConsumer<String> consumer = client.newConsumer(String.class)
        .subscriptionName("order-listener-sub")
        .subscriptionType(SubscriptionType.EXCLUSIVE)
        .messageListener((c, msg) -> {
            System.out.println("pushed: " + msg.getValue());
            c.acknowledge(msg);
        })
        .subscribe("persistent://public/default/order-topic");
```

---

## 与实现模块的关系

本模块是纯抽象 SPI，不含任何 Broker 连接代码；`MQClient` 实例由 [infrastructure-component-mq-pulsar](../infrastructure-component-mq-pulsar) 提供（基于 Apache Pulsar 客户端），传递依赖本模块。业务代码只需依赖 `:infrastructure-component-mq-api`，运行时替换实现模块即可。

---

## 注意事项

- `IProducer` 和 `IConsumer` 均实现（或等价于）`AutoCloseable`，建议使用 try-with-resources 或在 Bean 销毁钩子中显式关闭，避免连接泄漏。
- 标注 `@Deprecated` 的方法（`create()`、`subscribe()`、`loadConf(Map)`、`getStats()`）在当前 SPI 版本中已不推荐直接使用，请改用带 topic 参数的 `create(String topicName)` 和 `subscribe(String... topic)`，以及 `conf(Consumer<...>)` 配置方式。
- `MessageType` 枚举（`PULSAR` / `KAFKA` / `JSON`）为预留类型标志，当前实现尚未在接口方法中使用。
- `ProcessorBuilder` 目前仅为内部组合占位，尚未形成完整 API，不建议在业务代码中使用。
