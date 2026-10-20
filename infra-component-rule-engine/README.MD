# infra-component-rule-engine

QLExpress 4.1.3 的薄封装，包名 `org.carl.infra.ruleengine`。公共入口不暴露原始 runner 或厂商配置：

- `ConditionCompiler.compile(Map<String, ?>)`：将应用已绑定的 JSON 条件树编译为嵌套 record `CompiledCondition(script, parameters)`。
- `ExpressionOptions(long timeoutMillis, boolean precise)`：不可变配置，`DEFAULT = (1000, false)`，超时必须为正数；`precise` 对应 QLExpress 高精度计算选项。
- `ExpressionFactory.getDefault()` / `get(ExpressionOptions)`：返回 `ExpressionExecutor`，按配置值相等复用实例；`ExpressionExecutor` 是公共 final 具体封装类，也可通过无参或 `long timeoutMillis` 构造器创建独立实例（precise=false），配置构造器仅供包内工厂使用。
- `ExpressionExecutor.check(String)`：复用执行 runner，拒绝 null/空白，仅检查语法；原样传播 QLExpress 语法异常（行、列、错误码）。不执行脚本、不校验业务。
  `execute(String, Map<String, ?>)` / `execute(CompiledCondition, Map<String, ?>)`：返回原始结果 `Object`，运行时错误原样传播。

## 使用

```java
ExpressionExecutor executor = ExpressionFactory.getDefault(); // 1000ms，precise=false
ExpressionExecutor preciseExecutor = ExpressionFactory.get(new ExpressionOptions(2000L, true));
executor.check("user.level == 'VIP'");

ConditionCompiler compiler = new ConditionCompiler(executor); // 无参构造使用工厂默认实例
var condition = compiler.compile(Map.of(
    "combinator", "and",
    "rules", List.of(
        Map.of("field", "user.level", "operator", "==", "value", "VIP"),
        Map.of("field", "amount", "operator", ">=", "value", 100))));

Object matched = executor.execute(condition,
    Map.of("user", Map.of("level", "VIP"), "amount", 150)); // true
Object raw = executor.execute("amount * 2", Map.of("amount", 150)); // 300
```

## 条件树契约

- 组严格为 `{combinator: "and" | "or", rules: [节点, ...]}`；至少一个子节点。
- 叶严格为 `{field: "user.level", operator: "==", value: "VIP"}`。
- 操作符仅 `== != > >= < <= in`；不提供 `=` 别名。
- `value` 必须存在，显式 null 合法（Java 用可容纳 null 的 Map）。`in` 通常传 JSON 数组；类型兼容、比较、缺失事实语义由 QLExpress 决定，不额外做业务验证。
- 未知属性、混合组/叶、非法结构一律拒绝；根节点计为第 1 层，最大 32 层。
- 路径每段为 `[A-Za-z_][A-Za-z0-9_]*`，用点分隔；不接受索引、调用、表达式。根名称还必须通过 QLExpress 语法校验（不能是关键字）；后续段使用安全的 Map 下标访问。
- 条件和组完整加括号。值只绑定到参数 Map，绝不插入脚本文本。
- `__condition_` 是保留根命名空间：字段根及编译条件执行时的 facts 顶层键都不得使用该前缀；参数合并遇到任意同名 facts 键（包括 null）立即报错。
- 参数 Map 是不可修改的浅快照，嵌套值仍共享；调用者不得并发修改这些值。手工构造的 `CompiledCondition` 与原始脚本同样属于调用者负责的脚本输入，不是可信编译证明。

## 运行与边界

工厂通过 `ConcurrentHashMap.computeIfAbsent` 按不可变配置值复用实例（包括默认配置）。**工厂缓存无界、无淘汰，生命周期与进程一致，仅用于有限的应用自有配置，不可直接接收任意公网输入配置**；每个 runner 的表达式解析缓存同样无界，应控制脚本种类。

实例可复用，每个 executor 持有一个共享 runner，内部固定默认初始化/隔离策略、解析缓存开启、正数超时、所选 `precise`、`polluteUserContext(false)`。`execute` 不会自动先调用 `check`；`ConditionCompiler` 则始终使用其绑定 executor 校验生成脚本。每次执行复制顶层 Map，脚本局部赋值不写回调用者顶层 facts；**这不是深拷贝，也不是深层只读保证**。推荐 facts 使用 JSON 风格 Map/List/标量，并在执行期间保持嵌套对象稳定；独立调用使用独立 facts。超时由 QLExpress 协作检查，不是操作系统硬中断，也不限制内存或解析缓存规模。

语法成功不代表运行时成功：不存在的函数、不兼容的值、缺失事实、隔离策略限制等仍可能影响运行。没有业务函数注册、副作用抽象、动作、监听器、元数据或执行轨迹。

应用负责 JSON 解析/绑定、规则存储、授权和业务动作；不提供持久化或 SaaS 服务。模块不引入 Jackson，不使用 DTO 自动绑定或反射路径访问，不注册 native 反射配置。历史上旧 API 的 Map 路径在 macOS arm64、Quarkus 3.19.3、GraalVM CE 21.0.2 完成过 native 验证（临时工程 `/tmp/rulecmp/module-native`，不随仓库交付）；**当前工厂 API 尚未进行 native 验证**，不能沿用该结论。其他平台和 POJO 反射访问亦不在验证范围。

本模块没有新建异常层次：结构/参数校验仍抛出既有 Java 异常，QLExpress 语法和运行时异常原样传播。封装类只隔离执行入口，DSL、精度及异常语义仍依赖 QLExpress，不承诺透明切换脚本引擎或脚本迁移。

## 破坏性变更

此前的 `RuleEngine`、`Rule`、`RuleSet`、`Facts`、lambda DSL、动作/监听器、`IRuleEngineAbility`、JSON 字符串解析、元数据/轨迹 API 全部移除。迁移为应用绑定 Map → `ConditionCompiler` → `ExpressionExecutor`（经 `ExpressionFactory` 获取或直接构造），或直接执行脚本；原 `QLExpressExecutor` 已合并为 `ExpressionExecutor`，不再保留接口/实现拆分；业务编排由应用承担，不提供兼容层。

## 后续计划：类型安全的 Java 泛型 DSL（未实现）

保留现有字符串脚本入口，后续增加基于业务类型的链式规则声明：字段比较具备类型检查与 IDE 补全，支持条件组合及 then/otherwise 返回分支，最终针对业务对象执行。目标不是泛型返回值转换，也不是把 POJO 直接传给字符串脚本。

目标用法示意（非现有可编译 API）：

```java
var script = Script.of(Order.class)
    .facts(s -> s.amount.lt(200))
    .then(() -> true)
    .otherwise(() -> false);

boolean result = script.execute(order);
```

- `s.amount` 是与 `Order.amount` 对应的类型化字段描述，不是字段实际值。
- Java 不能仅凭泛型自动提供 `s.amount`；后续需评估生成字段描述代码的方案。方法引用式 `field(Order::getAmount).lt(200)` 为无需生成字段代码的备选，尚未决定。
- 设计时明确字段类型、嵌套属性、空值、条件组合、分支惰性求值及并发复用契约；保持计算用途，不新增业务动作或监听器体系。
- 与现有条件树/表达式执行层如何衔接、是否编译成脚本，留待设计确认；对外不暴露厂商 API。
- 实现前确认具体 API；实现后补类型检查、分支及边界测试，并重新实际验证 native。当前仅记录计划，不修改运行行为。

## 验收

```bash
./gradlew :infra-component-rule-engine:test :infra-component-rule-engine:build
```

公共 QLExpress 文档可能跟随 main 分支演进；当前依赖固定 4.1.3，以本地版本及测试为准。
