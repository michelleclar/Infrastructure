# infrastructure-component-quarkus / broadcast

把 Vert.x EventBus 包装成一个可直接注入 Quarkus CDI 应用的进程内消息总线（publish / subscribe / request-reply）。
业务 bean 只需注入 `BroadcastService` 或实现 `IBroadcastAbility`，即可在任意 CDI 组件间通过 topic 字符串解耦地收发消息，无需直接依赖 `EventBus`。

> 本模块是**进程内**总线，底层为 Vert.x `EventBus`，不跨节点，也不经 SSE/WebSocket 向客户端推送。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:broadcast"))
}
```

本模块自动传递 `quarkus-vertx`。

---

## 激活开关（构建期属性）

| 属性 | 说明 |
|------|------|
| `quarkus.plugins.broadcast.enable=true` | 必须在 `application.properties` 中设置，否则 `BroadcastService` bean **不会**被 CDI 容器注册（由 `@IfBuildProperty` 控制） |

```properties
# src/main/resources/application.properties
quarkus.plugins.broadcast.enable=true
```

> 注意：这是**构建期**属性（`@IfBuildProperty`），不能在运行时动态切换。

---

## 核心类型

| 类型 | 作用 |
|------|------|
| `BroadcastService` | `@ApplicationScoped` bean，核心入口，直接注入使用 |
| `BroadcastStd` | `BroadcastService` 的父类，可继承后在测试/扩展中使用 |
| `IBroadcastOperations` | 定义所有操作方法（`publish` / `subscribe` / `request` / `respond` / `unsubscribe`）的接口 |
| `IBroadcastAbility` | 组合接口，业务 bean 实现它并注入 `IBroadcastOperations`，即可获得发布/订阅能力 |
| `IBroadcastProvider` | 低层接口，提供 `BroadcastContext`（持有 `EventBus`） |
| `BroadcastRegistry` | 全局单例，维护 `topic -> MessageConsumer<?>` 的 `ConcurrentHashMap`；`subscribe` 时自动注册，`unsubscribe` 时移除 |
| `BroadcastContext` | 对 `io.vertx.mutiny.core.eventbus.EventBus` 的轻量包装 |
| `EventBusUtils` | 工具类，判断消息类型是否为 Vert.x 内置编解码类型，并生成对应的 `DeliveryOptions` |

---

## 使用示例

### 1. 直接注入 `BroadcastService`

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.infrastructure.broadcast.BroadcastService;

@ApplicationScoped
public class NotificationService {

    @Inject
    BroadcastService broadcast;

    // fire-and-forget 发布
    public void notifyOrderCreated(String orderId) {
        broadcast.publish("order.created", orderId);
    }

    // 发布后等待某个响应者回应（request-reply）
    public void queryInventory(String sku) {
        broadcast.request("inventory.query", sku)
                .subscribe().with(reply -> System.out.println("库存: " + reply));
    }

    // 订阅 topic（会先取消旧的同名订阅，再重新注册）
    public void listenPaymentEvents() {
        broadcast.subscribe("payment.success", String.class, orderId -> {
            System.out.println("支付成功，订单: " + orderId);
        }).subscribe().with(__ -> {});
    }

    // 注销全部订阅
    public void stop() {
        broadcast.unsubscribeAll().await().indefinitely();
    }
}
```

### 2. 实现 `IBroadcastAbility`（组合方式）

业务 bean 实现 `IBroadcastAbility`，注入 `BroadcastService` 提供给接口，无需继承：

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.infrastructure.broadcast.BroadcastService;
import org.carl.infrastructure.broadcast.IBroadcastOperations;
import org.carl.infrastructure.broadcast.ability.IBroadcastAbility;

@ApplicationScoped
public class OrderProcessor implements IBroadcastAbility {

    @Inject
    BroadcastService broadcastService;

    @Override
    public IBroadcastOperations getBroadcastOperations() {
        return broadcastService;
    }

    public void process(String orderId) {
        publish("order.processing", orderId);   // IBroadcastAbility 默认方法
    }
}
```

### 3. request-reply（带响应者）

```java
// 注册响应者（isObservable=true 表示同一 topic 只注册一次）
broadcast.respond(
    "inventory.query",
    String.class,
    Integer.class,
    sku -> Uni.createFrom().item(42),   // 返回库存数量
    true
).await().indefinitely();

// 发起请求，类型化接收返回值
broadcast.request("inventory.query", "SKU-001", Integer.class)
    .subscribe().with(qty -> System.out.println("库存: " + qty));
```

### 4. 自定义消息类型的 `DeliveryOptions`

Vert.x EventBus 内置编解码类型（`String`、`Integer`、`Long`、`Boolean`、`JsonObject`、`JsonArray`、`Buffer` 等）可直接发送；自定义 POJO 需要配合编解码器。`EventBusUtils` 提供辅助判断：

```java
import io.vertx.core.eventbus.DeliveryOptions;
import org.carl.infrastructure.broadcast.utils.EventBusUtils;

// 根据类型自动选择附带 isBaseType header 的 DeliveryOptions
DeliveryOptions opts = EventBusUtils.getIsBaseTypeDeliveryOptions(MyPojo.class);
broadcast.publish("my.topic", myPojo, opts);
```

---

## IBroadcastOperations 方法一览

| 方法签名 | 说明 |
|----------|------|
| `publish(String topic, T message)` | fire-and-forget 广播，返回 `EventBus`（支持链式调用） |
| `publish(String topic, T message, DeliveryOptions options)` | 同上，携带自定义投递选项 |
| `request(String topic, T message)` → `Uni<Object>` | 请求-回复，返回响应体 |
| `request(String topic, T message, DeliveryOptions options)` → `Uni<Object>` | 同上，携带投递选项 |
| `request(String topic, Object message, Class<R> ignore)` → `Uni<R>` | 请求-回复，类型化返回 |
| `respond(String topic, Class<Req>, Class<Res>, Function<Req, Uni<Res>>, boolean isObservable)` → `Uni<Void>` | 注册响应者；`isObservable=true` 时同一 topic 只注册一次 |
| `subscribe(String topic, Class<T> ignore, Consumer<T> handler)` → `Uni<Void>` | 订阅 topic（自动先取消旧的同名订阅） |
| `unsubscribe(String topic)` → `Uni<Void>` | 取消指定 topic 的订阅 |
| `unsubscribeAll()` → `Uni<Void>` | 取消 `BroadcastRegistry` 中所有订阅 |

---

## 注意事项

1. **构建期开关**：`quarkus.plugins.broadcast.enable=true` 缺失时，`BroadcastService` 不进入 CDI 容器，注入会报 `UnsatisfiedResolutionException`。
2. **进程内总线**：消息不跨 JVM，不持久化，重启后已注册的 `subscribe` 需重新注册。
3. **`subscribe` 的覆盖语义**：同一 topic 调用 `subscribe` 会先 `unsubscribe` 旧消费者，再注册新消费者，避免重复监听。
4. **自定义 POJO 编解码**：发送非内置类型的 POJO 前，需向 Vert.x EventBus 注册对应的 `MessageCodec`，否则运行时报序列化错误。`EventBusUtils.isDefaultSupportType(Class<?>)` 可用于判断是否为内置支持类型。
5. **Reactive 订阅**：`subscribe(...)` 返回 `Uni<Void>`，必须调用 `.subscribe().with(...)` 或 `.await().indefinitely()` 驱动执行，否则订阅不会生效。
