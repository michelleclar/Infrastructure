# infrastructure-component-pdp

> 轻量级策略决策点（Policy Decision Point）组件。以 `subject / action / resource` 三元组为输入，将一组可插拔的 `Policy` 规则链式求值，返回 `PERMIT` 或 `DENY`。无外部依赖，纯 Java 实现。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-pdp"))
}
```

---

## 核心 API

| 类型 | 全限定名 | 说明 |
|------|----------|------|
| `PolicyRequest` | `org.carl.infrastructure.pdp.PolicyRequest` | 请求三元组：`subject`、`action`、`resource`（record） |
| `PolicyDecision` | `org.carl.infrastructure.pdp.PolicyDecision` | 决策结果枚举：`PERMIT` / `DENY` |
| `Policy` | `org.carl.infrastructure.pdp.Policy` | 单条策略函数接口：`PolicyDecision evaluate(PolicyRequest)` |
| `Pdp` | `org.carl.infrastructure.pdp.Pdp` | 决策引擎接口，提供两个 `evaluate` 重载 |
| `DefaultPdp` | `org.carl.infrastructure.pdp.impl.DefaultPdp` | `Pdp` 的默认实现，接收 `List<Policy>` |
| `IPdpAbility` | `org.carl.infrastructure.pdp.IPdpAbility` | Mixin 接口，宿主类 implement 后直接调用 `evaluate(...)` |

---

## 求值语义

`DefaultPdp` 对策略列表逐条求值，规则如下：

- 任意一条策略返回 `DENY` → 立即短路，整体返回 `DENY`
- 至少一条策略返回 `PERMIT` 且无 `DENY` → 返回 `PERMIT`
- 策略列表为空，或没有任何 `PERMIT` → 返回 `DENY`（默认拒绝）

---

## 使用示例

### 1. 直接使用 `DefaultPdp`

```java
import org.carl.infrastructure.pdp.DefaultPdp; // 注意：实际导入用 impl 子包
import org.carl.infrastructure.pdp.Pdp;
import org.carl.infrastructure.pdp.Policy;
import org.carl.infrastructure.pdp.PolicyDecision;
import org.carl.infrastructure.pdp.PolicyRequest;
import org.carl.infrastructure.pdp.impl.DefaultPdp;

import java.util.List;

// 定义策略：只允许 alice 读取 document 资源
Policy onlyAliceCanRead = req ->
    "alice".equals(req.subject()) && "read".equals(req.action())
        ? PolicyDecision.PERMIT
        : PolicyDecision.DENY;

Pdp pdp = new DefaultPdp(List.of(onlyAliceCanRead));

PolicyDecision result = pdp.evaluate("alice", "read", "document:1");
// result == PolicyDecision.PERMIT

PolicyDecision denied = pdp.evaluate("bob", "read", "document:1");
// denied == PolicyDecision.DENY
```

### 2. 通过 `PolicyRequest` 传参

```java
PolicyRequest request = new PolicyRequest("alice", "delete", "document:1");
PolicyDecision decision = pdp.evaluate(request);
```

### 3. 使用 `IPdpAbility` Mixin

将 `IPdpAbility` 混入业务服务类，覆写 `getPdp()` 注入带策略的实例，即可在服务内直接调用 `evaluate()`：

```java
import org.carl.infrastructure.pdp.IPdpAbility;
import org.carl.infrastructure.pdp.Pdp;
import org.carl.infrastructure.pdp.PolicyDecision;
import org.carl.infrastructure.pdp.PolicyRequest;
import org.carl.infrastructure.pdp.impl.DefaultPdp;

import java.util.List;

public class DocumentService implements IPdpAbility {

    private final Pdp pdp = new DefaultPdp(List.of(
        req -> "alice".equals(req.subject()) ? PolicyDecision.PERMIT : PolicyDecision.DENY
    ));

    @Override
    public Pdp getPdp() {
        return pdp;
    }

    public void deleteDocument(String userId, String docId) {
        PolicyDecision decision = evaluate(new PolicyRequest(userId, "delete", docId));
        if (decision == PolicyDecision.DENY) {
            throw new SecurityException("无权限：" + userId + " 不允许删除 " + docId);
        }
        // 执行删除 ...
    }
}
```

---

## 注意事项

- **默认拒绝原则**：策略列表为空时求值结果为 `DENY`，不会误放行。
- **DENY 优先**：只要有一条策略返回 `DENY`，无论其他策略结果如何，最终结果即为 `DENY`。
- **`IPdpAbility.getPdp()` 默认实现**接口默认返回 `new DefaultPdp(List.of())`，即空策略列表（全拒绝）。生产使用时**必须覆写 `getPdp()`** 并传入实际策略。
- 本组件无 CDI / Quarkus 依赖，可在任意 Java 21 环境中使用。若需与 Quarkus Bean 集成，自行在 `@ApplicationScoped` Bean 中构造 `DefaultPdp` 并注入。
