# infra-component-rule-engine

QLExpress 4.1.3 的薄封装，只有三个顶层类，包名 `org.carl.infra.ruleengine`：

- `ScriptValidator.check(String)`：拒绝 null/空白，仅检查语法；原样传播 QLExpress 语法异常（行、列、错误码）。不执行脚本、不校验业务。
- `ConditionCompiler.compile(Map<String, ?>)`：将应用已绑定的 JSON 条件树编译为嵌套 record `CompiledCondition(script, parameters)`。
- `QLExpressExecutor.execute(String, Map<String, ?>)` / `execute(CompiledCondition, Map<String, ?>)`：返回原始结果 `Object`，运行时错误原样传播。

## 使用

```java
ScriptValidator validator = new ScriptValidator();
validator.check("user.level == 'VIP'");

ConditionCompiler compiler = new ConditionCompiler();
var condition = compiler.compile(Map.of(
    "combinator", "and",
    "rules", List.of(
        Map.of("field", "user.level", "operator", "==", "value", "VIP"),
        Map.of("field", "amount", "operator", ">=", "value", 100))));

QLExpressExecutor executor = new QLExpressExecutor(1000L); // 正数毫秒，默认 1000
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

实例可复用，每个 executor 持有一个共享 runner，默认隔离策略、解析缓存开启、正数超时、`polluteUserContext(false)`。每次执行复制顶层 Map，脚本局部赋值不写回调用者顶层 facts；**这不是深拷贝，也不是深层只读保证**。推荐 facts 使用 JSON 风格 Map/List/标量，并在执行期间保持嵌套对象稳定；独立调用使用独立 facts。超时由 QLExpress 协作检查，不是操作系统硬中断，也不限制内存或解析缓存规模。

语法成功不代表运行时成功：不存在的函数、不兼容的值、缺失事实、隔离策略限制等仍可能影响运行。没有业务函数注册、副作用抽象、动作、监听器、元数据或执行轨迹。

应用负责 JSON 解析/绑定、规则存储、授权和业务动作。模块不引入 Jackson，不使用 DTO 自动绑定或反射路径访问，不注册 native 反射配置。已在 macOS arm64、Quarkus 3.19.3、GraalVM CE 21.0.2 实际构建并运行 native image：通过语法校验、条件树、in/null、值注入防护、顶层上下文隔离、脚本 lambda、超时及 16,000 次并发执行。验证工程位于 `/tmp/rulecmp/module-native`（临时文件，不随仓库交付）；其他平台和 POJO 反射访问不在本次验证范围。

## 破坏性变更

此前的 `RuleEngine`、`Rule`、`RuleSet`、`Facts`、lambda DSL、动作/监听器、`IRuleEngineAbility`、JSON 字符串解析、元数据/轨迹 API 全部移除。迁移为应用绑定 Map → `ConditionCompiler` → `QLExpressExecutor`，或直接执行脚本；业务编排由应用承担，不提供兼容层。

## 验收

```bash
./gradlew :infra-component-rule-engine:clean :infra-component-rule-engine:test :infra-component-rule-engine:build
```

公共 QLExpress 文档可能跟随 main 分支演进；当前依赖固定 4.1.3，以本地版本及测试为准。
