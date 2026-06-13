# infrastructure-component-quarkus / audit

为 Quarkus CDI 应用提供轻量级审计能力：通过 `IAuditAbility` 接口混入任意业务 bean，
记录实体操作日志（操作人 / 动作 / 时间戳），并支持按实体类型 + 实体 ID 查询历史记录。

审计存储由 `AuditContext`（内存 `ConcurrentLinkedDeque`）驱动，操作人通过 `ThreadLocal` 传递，
无需改动调用链签名。

---

## 依赖

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:audit"))
}
```

本模块只依赖 `quarkus-arc`，不引入任何额外传递依赖。

---

## 构建开关

`AuditService` bean 受编译期属性控制，**默认不激活**，需在 `application.properties` 中显式开启：

```properties
quarkus.plugins.audit.enable=true
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `quarkus.plugins.audit.enable` | `String` | （未设置） | 设为 `"true"` 时才会将 `AuditService` 注册为 CDI bean |

> 这是 `@IfBuildProperty` 构建时开关，**更改后需重新编译**，运行期修改无效。

---

## 核心类 / 接口

| 类 / 接口 | 作用 |
|-----------|------|
| `IAuditOperations` | 审计操作核心接口：`record(...)` 写入、`query(...)` 查询 |
| `AuditEvent` | 审计记录模型（见下表） |
| `AuditContext` | `IAuditOperations` 的内存实现；同时管理操作人 `ThreadLocal` |
| `AuditContextProvider` | CDI `@Produces`，生产 `@ApplicationScoped` 的 `AuditContext` bean |
| `IAuditAbility` | 业务 bean 混入接口，暴露 `auditRecord` / `auditQuery` 默认方法 |
| `AuditStd` | 实现 `IAuditAbility` 的抽象基类，注入 `AuditContext`，业务 bean 继承即用 |
| `AuditService` | 继承 `AuditStd` 的 `@ApplicationScoped` bean，受构建开关控制 |

### `AuditEvent` 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `long` | 写入时由 `AuditContext` 自动赋值（原子自增） |
| `entityType` | `String` | 实体类型，如 `"Order"` |
| `entityId` | `String` | 实体 ID，如 `"O-1"` |
| `action` | `String` | 操作动作，如 `"CREATE"` / `"UPDATE"` / `"DELETE"` |
| `operator` | `String` | 操作人；快捷 `record` 方法自动从 ThreadLocal 读取，默认值 `"system"` |
| `details` | `String` | 附加说明（可为 `null`） |
| `timestamp` | `Instant` | 操作时间；快捷 `record` 方法自动记录 `Instant.now()` |

---

## 使用方式

### 方式一：注入 `AuditService`（推荐）

开启构建开关后，直接注入 `AuditService`：

```java
@ApplicationScoped
public class OrderService {

    @Inject
    AuditService audit;

    public void createOrder(String orderId, String operator) {
        // 设置当前线程的操作人
        AuditContext.setCurrentOperator(operator);
        try {
            // … 业务逻辑 …

            // 写入审计：entityType="Order", entityId=orderId, action="CREATE"
            audit.auditRecord("Order", orderId, "CREATE", "new order created");
        } finally {
            AuditContext.clearCurrentOperator();
        }
    }

    public List<AuditEvent> history(String orderId) {
        return audit.auditQuery("Order", orderId);
    }
}
```

### 方式二：业务 bean 继承 `AuditStd`

如果业务类本身希望成为 CDI bean 并内嵌审计能力，可直接继承 `AuditStd`：

```java
@ApplicationScoped
public class InvoiceService extends AuditStd {

    public void issue(String invoiceId) {
        // AuditStd 已注入 AuditContext，直接调用混入方法
        auditRecord("Invoice", invoiceId, "ISSUE", null);
    }
}
```

### 方式三：直接注入 `AuditContext`

`AuditContext` 本身是 `@ApplicationScoped` bean，可直接注入以调用 `IAuditOperations`：

```java
@Inject
AuditContext auditContext;

// 完整构造 AuditEvent
AuditEvent event = new AuditEvent(
        0L, "Payment", "PAY-99", "REFUND", "alice", "refund approved", Instant.now());
auditContext.record(event);

// 查询
List<AuditEvent> events = auditContext.query("Payment", "PAY-99");
```

---

## 操作人（operator）传递

`AuditContext` 通过 `ThreadLocal` 持有当前操作人，默认值为 `"system"`。
快捷 `record(entityType, entityId, action, details)` 方法自动读取该值。

| 方法 | 作用 |
|------|------|
| `AuditContext.setCurrentOperator(String operator)` | 设置当前线程的操作人 |
| `AuditContext.getCurrentOperator()` | 读取当前线程的操作人 |
| `AuditContext.clearCurrentOperator()` | 清除 ThreadLocal（请在 finally 块中调用，防止线程复用污染） |

典型做法是在请求入口（如 JAX-RS 过滤器 / CDI interceptor）统一设置：

```java
// 示例：JAX-RS ContainerRequestFilter
@Override
public void filter(ContainerRequestContext ctx) {
    String user = resolveUserFromToken(ctx);
    AuditContext.setCurrentOperator(user);
}
```

---

## 注意事项

- `AuditContext` 使用**内存** `ConcurrentLinkedDeque` 存储，进程重启后记录丢失。如需持久化，应在应用层将查询结果导出到数据库或消息队列。
- `query` 方法要求 `entityType` 和 `entityId` 均不为 `null`，否则抛出 `NullPointerException`。
- 构建开关 `quarkus.plugins.audit.enable=true` 控制的是 `AuditService` bean；`AuditContext` 和 `AuditContextProvider` 无论开关状态始终存在于容器中。
- `ThreadLocal` 在线程池场景下必须在请求结束时调用 `AuditContext.clearCurrentOperator()`，否则操作人会被后续请求继承。
