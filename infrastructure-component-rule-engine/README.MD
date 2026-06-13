# infrastructure-component-rule-engine

> 轻量级自研规则引擎（灵感来自 [COLA](https://github.com/alibaba/COLA)，无第三方规则引擎依赖）。将业务判断逻辑封装为可组合的 `Rule`，通过 `RuleEngine` 触发执行，事实（fact）通过 `Facts` 传入传出。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-rule-engine"))
}
```

---

## 核心概念

| 类 / 接口 | 包 | 说明 |
|---|---|---|
| `Fact<T>` | `api` | 单条命名事实，持有 `name` + `value`（均不可为 null） |
| `Facts` | `api` | 事实容器（`Set` 语义，同名覆盖），引擎执行时作为输入/输出共享载体 |
| `Condition` | `api` | `@FunctionalInterface`，`boolean evaluate(Facts facts)`；内置 `TRUE` / `FALSE` 常量，支持 `.and()` / `.or()` 组合 |
| `Action` | `api` | `@FunctionalInterface`，`void execute(Facts facts)` |
| `Rule` | `api` | 接口，含 `evaluate` / `execute` / `apply`（= evaluate + execute）；实现 `Comparable` 按优先级排序 |
| `RuleEngine` | `api` | 接口，入口为 `void fire(Rule rule, Facts facts)` |
| `RuleBuilder` | `core` | 流式 Builder，组装 `Condition` + 多个 `Action` 生成 `DefaultRule` |
| `DefaultRuleEngine` | `core` | `RuleEngine` 默认实现，委托给 `rule.apply(facts)` |
| `AllRules` | `core` | 组合规则（AND 语义）：所有子规则条件全部满足才执行 |
| `AnyRules` | `core` | 组合规则（OR 短路语义）：第一个 `apply` 成功即返回，后续不再执行 |
| `NaturalRules` | `core` | 组合规则（顺序语义）：按优先级依次 `apply` 所有子规则，不做短路 |
| `AbstractRule` | `core` | 继承基类，子类覆写 `evaluate` / `execute` / `apply` |
| `IRuleEngineAbility` | 根包 | Mixin 接口，实现类可免费获得共享 `DEFAULT_ENGINE` 以及 `fireRule(rule, facts)` 便捷方法 |

---

## 使用方式

### 1. 用 `RuleBuilder` 定义叶子规则

```java
import org.carl.infrastructure.ruleengine.api.*;
import org.carl.infrastructure.ruleengine.core.*;

// 准备事实
Facts facts = new Facts();
facts.put("number", 15);

// 定义规则
Rule fizzRule = new RuleBuilder()
        .name("fizzRule")
        .description("能被 3 整除则输出 Fizz")
        .priority(10)
        .when(f -> (int) f.get("number") % 3 == 0)
        .then(f -> f.put("fizz", "Fizz"))
        .build();

Rule buzzRule = new RuleBuilder()
        .name("buzzRule")
        .description("能被 5 整除则输出 Buzz")
        .priority(20)
        .when(f -> (int) f.get("number") % 5 == 0)
        .then(f -> f.put("buzz", "Buzz"))
        .build();

Rule defaultRule = new RuleBuilder()
        .name("defaultRule")
        .description("兜底：直接输出数字")
        .priority(40)
        .when(f -> true)
        .then(f -> System.out.println(f.get("number")))
        .build();

// 执行
RuleEngine engine = new DefaultRuleEngine();
engine.fire(fizzRule, facts);
System.out.println(facts.get("fizz")); // "Fizz"
```

---

### 2. 组合规则

#### AllRules（AND 语义）

所有子规则的 `evaluate` 均为 true 时，才依次执行所有子规则的 `execute`。

```java
// fizzRule 与 buzzRule 都满足 → 同时输出 fizz 和 buzz
Rule fizzBuzzRule = AllRules.allOf(fizzRule, buzzRule)
        .name("fizzBuzzRule")
        .priority(5);
```

#### AnyRules（OR 短路语义）

按优先级排序后，第一个 `apply` 返回 true 的子规则执行后立即停止。

```java
// 优先级：fizzBuzzRule(5) < fizzRule(10) < buzzRule(20) < defaultRule(40)
// 传入 15 → fizzBuzzRule 最先命中并执行，后续不再执行
Rule rootRule = AnyRules.anyOf(fizzBuzzRule, fizzRule, buzzRule, defaultRule)
        .name("anyRule");

engine.fire(rootRule, facts);
```

#### NaturalRules（顺序语义）

按优先级排序后，无论条件是否满足，依次对每条子规则调用 `apply`（不做短路）。

```java
Rule natural = NaturalRules.of(ruleA, ruleB, ruleC).name("naturalRule");
engine.fire(natural, facts);
```

---

### 3. 继承 `AbstractRule` 自定义规则类

```java
public class MyRule extends AbstractRule {

    public MyRule() {
        super("myRule", "自定义规则示例", 1);
    }

    @Override
    public boolean evaluate(Facts facts) {
        Integer score = facts.get("score");
        return score != null && score >= 60;
    }

    @Override
    public void execute(Facts facts) {
        facts.put("result", "passed");
    }

    @Override
    public boolean apply(Facts facts) {
        if (evaluate(facts)) {
            execute(facts);
            return true;
        }
        return false;
    }
}
```

---

### 4. 通过 `IRuleEngineAbility` Mixin 使用

业务 Service 实现 `IRuleEngineAbility` 即可复用共享的 `DefaultRuleEngine` 静态实例，无需手动构造引擎。

```java
import org.carl.infrastructure.ruleengine.IRuleEngineAbility;
import org.carl.infrastructure.ruleengine.api.*;
import org.carl.infrastructure.ruleengine.core.RuleBuilder;

public class ScoreService implements IRuleEngineAbility {

    public String check(int score) {
        Facts facts = new Facts();
        facts.put("score", score);

        Rule passRule = new RuleBuilder()
                .when(f -> (int) f.get("score") >= 60)
                .then(f -> f.put("result", "passed"))
                .build();

        fireRule(passRule, facts);           // 来自 IRuleEngineAbility
        return facts.get("result");
    }
}
```

---

## Facts API 速查

| 方法 | 说明 |
|---|---|
| `put(String name, T value)` | 添加或覆盖同名事实 |
| `add(Fact<T> fact)` | 同上，接受 `Fact` 对象 |
| `get(String factName)` | 按名称取值（泛型，未找到返回 `null`） |
| `getFact(String factName)` | 取 `Fact<?>` 对象本身 |
| `remove(String factName)` | 按名称删除 |
| `contains(String factName)` | 判断是否存在 |
| `asMap()` | 导出为 `Map<String, Object>` 快照 |
| `clear()` | 清空所有事实 |

---

## 注意事项

- **优先级数值越小，优先级越高**（`CompositeRule` 内部对子规则执行 `Collections.sort`，`Comparable` 以 `priority` 升序排列）。
- `AnyRules` 不支持直接调用 `execute(Facts)`（抛 `RuntimeException`），只能通过 `apply` / `fire` 使用。
- `NaturalRules` 不支持作为其他组合规则的子规则（`evaluate` / `execute` 均抛 `RuntimeException`），只能作为顶层规则交给 `RuleEngine.fire`。
- `Fact` 的 `name` 和 `value` 均不可为 null，违反时抛 `NullPointerException`。
- `IRuleEngineAbility.DEFAULT_ENGINE` 是接口字段（`public static final`），被所有实现类共享，线程安全（`DefaultRuleEngine` 本身无状态）。
