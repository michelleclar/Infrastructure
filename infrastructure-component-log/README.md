# infrastructure-component-log

> 统一日志门面模块。屏蔽 SLF4J 与 JBoss Logging 的 API 差异，提供单一的 `ILogger` 接口和 `LoggerFactory` 工厂，并通过 `ILogAbility` 混入接口让业务类零样板获取日志实例。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-log"))
}
```

运行时至少需要以下其中一个（模块本身以 `implementation` 方式引入，消费方可根据环境覆盖）：

| 框架 | 典型场景 |
|------|----------|
| `org.jboss.logging:jboss-logging` | Quarkus 环境（优先自动选择） |
| `org.slf4j:slf4j-api` + 实现绑定 | 非 Quarkus / 独立 Spring Boot 等 |

---

## 框架选择

### 自动检测顺序

`LoggerFactory` 在每次创建 logger 时按以下顺序决定底层实现：

1. **系统属性** `infrastructure.logging.framework`（值：`SLF4J` / `JBOSS_LOGGING` / `AUTO`）
2. **环境变量** `INFRASTRUCTURE_LOGGING_FRAMEWORK`（同上取值）
3. **代码显式设置**（`LoggerFactory.setLoggingFramework(...)`）
4. **类路径自动探测**：优先 JBoss Logging，fallback SLF4J

### 配置键一览

| 方式 | 键名 | 可选值 |
|------|------|--------|
| 系统属性 | `infrastructure.logging.framework` | `SLF4J` \| `JBOSS_LOGGING` \| `AUTO` |
| 环境变量 | `INFRASTRUCTURE_LOGGING_FRAMEWORK` | 同上 |

---

## 核心 API

### 包 `org.carl.infrastructure.logging`

| 类型 | 说明 |
|------|------|
| `ILogger` | 统一日志接口，提供 `trace` / `debug` / `info` / `warn` / `error` 五个级别的三种重载（纯消息、格式化、带 Throwable） |
| `LogLevel` | 日志级别枚举：`TRACE` `DEBUG` `INFO` `WARN` `ERROR` |
| `LoggerFactory` | 工厂类，带并发安全的 logger 缓存；提供 `getLogger(Class<?>)` / `getLogger(String)` |
| `LoggingFramework` | 框架枚举：`SLF4J` `JBOSS_LOGGING` `AUTO` |
| `LoggingConfig` | 配置持有类，支持链式调用和 Builder；调用 `apply()` 或 `buildAndApply()` 生效 |

### 包 `org.carl.infrastructure.logging.ability`

| 类型 | 说明 |
|------|------|
| `ILogAbility` | 混入接口；实现类直接调用 `getLogger()` 即可获得与本类绑定的 `ILogger`，无需声明字段 |

### 包 `org.carl.infrastructure.logging.adapter`（内部实现，通常不直接使用）

| 类型 | 说明 |
|------|------|
| `Slf4jLoggerAdapter` | 将 `org.slf4j.Logger` 适配为 `ILogger` |
| `JBossLoggerAdapter` | 将 `org.jboss.logging.Logger` 适配为 `ILogger`；格式化占位符同时支持 `{}` 和 `%s` |

---

## 使用示例

### 方式一：直接使用 `LoggerFactory`（推荐用于工具类、静态上下文）

```java
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;

public class PaymentService {

    private static final ILogger log = LoggerFactory.getLogger(PaymentService.class);

    public void pay(String orderId, double amount) {
        log.info("开始支付 orderId={}, amount={}", orderId, amount);
        try {
            // ... 业务逻辑
            log.debug("支付渠道响应: {}", "SUCCESS");
        } catch (Exception e) {
            log.error("支付失败 orderId={}", orderId, e);
        }
    }
}
```

### 方式二：实现 `ILogAbility`（推荐用于 CDI Bean / Service 类）

```java
import org.carl.infrastructure.logging.ability.ILogAbility;

public class OrderService implements ILogAbility {

    public void processOrder(String orderId, int quantity) {
        getLogger().info("Processing order: {} with quantity: {}", orderId, quantity);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Order details - ID: {}, Qty: {}", orderId, quantity);
        }

        getLogger().info("Order {} processed successfully", orderId);
    }

    public void cancelOrder(String orderId) {
        getLogger().warn("Cancelling order: {}", orderId);
    }
}
```

`ILogAbility.getLogger()` 每次通过 `LoggerFactory.getLogger(this.getClass())` 获取，已有缓存不会重复创建。

### 方式三：代码显式指定框架

```java
import org.carl.infrastructure.logging.LoggingConfig;
import org.carl.infrastructure.logging.LoggingFramework;

// 应用启动时执行一次
LoggingConfig.builder()
    .useSlf4j()          // 或 .useJBossLogging() / .autoDetect()
    .buildAndApply();
```

---

## 格式化占位符说明

`JBossLoggerAdapter` 的格式化方法同时识别两种占位符风格：

| 风格 | 示例 |
|------|------|
| `{}` 风格（SLF4J 兼容） | `log.info("user={}, action={}", userId, action)` |
| `%s` / `%d` 等 printf 风格 | `log.info("user=%s, count=%d", userId, count)` |

`Slf4jLoggerAdapter` 直接透传给 SLF4J，占位符遵循 SLF4J 的 `{}` 规则。

---

## 注意事项

- `LoggerFactory` 是无状态工具类，不可实例化；logger 实例以类名/字符串为键做 `ConcurrentHashMap` 缓存，线程安全。
- 调用 `LoggerFactory.setLoggingFramework(...)` 会**清空**已有缓存，下次 `getLogger` 时以新框架重新创建；测试时可用 `LoggerFactory.reset()` 恢复初始状态。
- 若类路径上两个框架都不存在，`LoggerFactory` 会抛出 `IllegalStateException`，启动期即可感知，不会静默失败。
- 本模块不引入 Quarkus CDI，可独立用于纯 Java 或任意 JVM 框架。
