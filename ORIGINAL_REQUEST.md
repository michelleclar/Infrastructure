# Original User Request

## Initial Request — 2026-07-07T12:20:17Z

Implement `infrastructure-component-mq-kafka`, a pure Java library module that implements the `infrastructure-component-mq-api` using Apache Kafka. It should follow the architectural design and patterns of `infrastructure-component-mq-pulsar`.

Working directory: /Users/carl/workspace/backend/Infrastructure/infrastructure-component-mq-kafka
Integrity mode: demo

## Requirements

### R1. Core Kafka Implementation
Implement Kafka-backed classes for all primary `mq-api` interfaces: `IProducer<T>`, `IProducerBuilder<T>`, `IConsumer<T>`, `IConsumerBuilder<T>`, `IReader<T>`, `IReaderBuilder<T>`, and `MQClient`. The top-level entry point should be a public `MQClientBuilder` class mirroring the Pulsar one, with a `createClient(MQConfig)` static factory method. Use `org.apache.kafka:kafka-clients` for the implementation.

### R2. Kafka-specific Config
Provide a `KafkaConfig` class that implements `MQConfig`. It should include sensible defaults for a Kafka broker at `localhost:9092`. The sub-configs should cover at minimum: `ClientConfig` (bootstrapServers, security, etc.), `ProducerConfig`, and `ConsumerConfig`. Use the same nested-class pattern as `PulsarConfig`.

### R3. Structural Parity with Pulsar Module
The new module must mirror the package structure of the Pulsar module:
- `org.carl.infrastructure.mq.kafka.builder` (for client, producer, consumer, reader builders and impls)
- `org.carl.infrastructure.mq.kafka.config` (for KafkaConfig, KafkaConfigValidator, ResourcesManager)
- The `build.gradle.kts` must declare `api(project(":infrastructure-component-mq-api"))` and use `kafka-clients` as implementation dependency. It must also include maven-publish configuration identical in structure to the Pulsar module's.
- The new module must be registered in the root `settings.gradle.kts`.

### R4. Message Mapping
Map `MessageBuilder<T>` fields to Kafka's `ProducerRecord` fields (key → record key, properties → headers, eventTime → timestamp). The `Message<T>` wrapper must expose `getSourceMessage()` returning the original Kafka `ConsumerRecord` for use in acknowledgement. Since Kafka consumers manage offsets via `commitSync()`/`commitAsync()`, the `acknowledge()` methods on `KafkaConsumer` must commit the specific offset of the provided message.

### R5. Testing with Testcontainers
Provide an integration test suite using Testcontainers with the `confluentinc/cp-kafka` or `apache/kafka` Docker image. Tests must cover:
1. A producer successfully publishing a message to a topic.
2. A consumer successfully receiving that message and verifying payload.
3. Typed (generic `T`) producer/consumer round-trip using JSON serialization (since Kafka has no built-in AVRO schema registry assumed by default).

---

## Reference: Key mq-api Interfaces (do NOT modify these files)

All interfaces live in `infrastructure-component-mq-api` at `/Users/carl/workspace/backend/Infrastructure/infrastructure-component-mq-api`.

Read the complete source of that module carefully before implementing. Key types:

### MQClient (client/MQClient.java)
```java
public abstract class MQClient {
    public abstract IProducerBuilder<byte[]> newProducer();
    public abstract <T> IProducerBuilder<T> newProducer(Class<T> clazz);
    public abstract IConsumerBuilder<byte[]> newConsumer();
    public abstract <T> IConsumerBuilder<T> newConsumer(Class<T> clazz);
    public abstract IReaderBuilder<byte[]> newReader();
    public abstract <T> IReaderBuilder<T> newReader(Class<T> clazz);
    public abstract void close() throws MQClientException;
    public abstract CompletableFuture<Void> closeAsync();
    public abstract void shutdown() throws MQClientException;
    public abstract boolean isClosed();
}
```

### IProducer<T> (producer/IProducer.java)
Key methods (implement all):
- `sendMessage(T value)` → `SendResult<T>`
- `sendMessage(T value, Consumer<MessageBuilder<T>> configurator)` → `SendResult<T>`
- `sendMessageAsync(T value)` → `CompletableFuture<SendResult<T>>`
- `sendMessageAsync(T value, Consumer<MessageBuilder<T>> configurator)` → `CompletableFuture<SendResult<T>>`
- `flush()`, `flushAsync()`, `isConnected()`, `getProducerName()`, `close()`, `closeAsync()`
- Batch/delayed/transaction variants can be stubs (empty body), same as the Pulsar impl.

### IConsumer<T> (consumer/IConsumer.java)
Key methods:
- `receive()` → `Message<T>`
- `receive(int timeout, TimeUnit unit)` → `Message<T>`
- `receiveAsync()` → `CompletableFuture<Message<T>>`
- `acknowledge(Message<T>)`, `acknowledgeAsync(Message<T>)`
- `negativeAcknowledge(Message<T>)` (no-op or seek back)
- `pause()`, `resume()`, `seek(long timestamp)`, `isConnected()`, `close()`, `closeAsync()`

### IReader<T> (reader/IReader.java)
Key methods:
- `readNext()` → `Message<T>`
- `readNext(int timeout, TimeUnit unit)` → `Message<T>`
- `readNextAsync()` → `CompletableFuture<Message<T>>`
- `hasMessageAvailable()` → `boolean`
- `seek(long timestamp)`, `isConnected()`, `close()`, `closeAsync()`

### Message<T> / MessageBuilder<T> (model/)
Use the existing API model classes. `Message<T>` is an interface. Provide a `KafkaMessage<T>` implementation. `MessageBuilder<T>` is a concrete class in the API that can be used directly.

### Exceptions (common/ex/)
Reuse `MQClientException`, `ProducerException`, `ConsumerException`, `ReaderException` from the API.

---

## Reference: Pulsar module builder structure (mirror this in Kafka)

The Pulsar module has this class structure at `/Users/carl/workspace/backend/Infrastructure/infrastructure-component-mq-pulsar`. Read its complete source before implementing.

Mirror this package pattern:
```
builder/
  MQClientBuilder.java        ← public entry point, createClient(MQConfig)
  KafkaClientFactory.java     ← internal pipeline builder (package-private)
  KafkaMQClient.java          ← MQClient impl (package-private)
  KafkaProducerBuilder.java   ← IProducerBuilder impl (package-private)
  KafkaProducer.java          ← IProducer impl (public)
  KafkaMessageBuilder.java    ← MessageBuilder impl + inner Message impl (package-private)
  KafkaConsumerBuilder.java   ← IConsumerBuilder impl (package-private)
  KafkaConsumer.java          ← IConsumer impl (package-private)
  KafkaReaderBuilder.java     ← IReaderBuilder impl (package-private)
  KafkaReader.java            ← IReader impl (package-private)
config/
  KafkaConfig.java            ← MQConfig impl with nested sub-config classes
  KafkaConfigValidator.java   ← validates config, logs warnings/errors
  ResourcesManager.java       ← ConcurrentHashMap registry of all MQClient instances
```

---

## Project context

- Java 21 toolchain
- Package: `org.carl.infrastructure.mq.kafka.*`
- 4-space indent, UTF-8, LF line endings
- Use `ILogger` from `org.carl.infrastructure.logging` for all logging (NOT SLF4J directly). The logger is available via `infrastructure-component-log` which is a transitive dependency through mq-api.
- Root project at `/Users/carl/workspace/backend/Infrastructure`
- Settings file: `/Users/carl/workspace/backend/Infrastructure/settings.gradle.kts`
- Aliyun Maven publish credentials: env vars `ALIYUN_MAVEN_USERNAME` / `ALIYUN_MAVEN_PASSWORD`
- Aliyun Maven URL: `https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey`
- Group: `org.carl`, Version: `1.0-BATE`

---

## Acceptance Criteria

### Build
- [ ] `./gradlew :infrastructure-component-mq-kafka:build` completes without errors.
- [ ] The module is included in `settings.gradle.kts`.

### Implementation Completeness
- [ ] `KafkaMQClient`, `KafkaProducer`, `KafkaConsumer`, `KafkaReader`, their respective builders, and `KafkaConfig` all exist and implement the correct `mq-api` interfaces.
- [ ] `MQClientBuilder.createClient(MQConfig)` returns a working `MQClient`.

### Integration Tests
- [ ] `./gradlew :infrastructure-component-mq-kafka:test` passes.
- [ ] At least one integration test using Testcontainers verifies a full produce→consume round trip with a real Kafka broker.
- [ ] At least one typed (`T` = a POJO) produce→consume round trip test passes using JSON serialization.
