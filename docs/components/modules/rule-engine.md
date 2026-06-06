# rule-engine

## 模块定位

`infrastructure-component-rule-engine` 是轻量规则引擎组件，提供规则、条件、动作、事实集合、组合规则和默认执行器。

## 核心能力

- api：`RuleEngine`、`Rule`、`Condition`、`Action`、`Fact`、`Facts`。
- core：`DefaultRuleEngine`、`DefaultRule`、`RuleBuilder`。
- 组合规则：`AllRules`、`AnyRules`、`CompositeRule`、`NaturalRules`。
- `IRuleEngineAbility`：能力 mixin。

## 依赖边界

- 不依赖 Quarkus、CDI、数据库、MQ 或外部规则服务。
- 可被 workflow、approval、authorization 等上层组件复用。
- 不应直接输出到 `System.err`，后续应改用日志组件或返回明确异常。

## 对外 API

- `RuleEngine#fire(Rule rule, Facts facts)`。
- `Rule#apply(Facts facts)`。
- `RuleBuilder` 构建规则。
- `Facts` 管理规则执行上下文。

## 典型使用场景

- 简单业务规则编排。
- 审批、权限或工作流中执行条件判断和动作。
- 测试中用内存规则替代复杂外部规则引擎。

## 维护事项

- 规则执行顺序和组合规则短路语义要保持稳定。
- 动作异常处理策略需要明确，是中断执行还是记录失败继续。
- 如果规则复杂度上升，应考虑表达式语言或外部规则引擎 adapter，而不是让 core 变成框架。

## 测试验收

- `./gradlew :infrastructure-component-rule-engine:test` 通过。
- all/any/composite/default rule 的命中和未命中路径有测试。
- 源码中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：提供轻量内存规则执行能力，让简单条件判断和动作编排不必引入重量级规则引擎。

**如何使用**：使用 `RuleBuilder` 构造规则，准备 `Facts` 上下文，然后通过 `new DefaultRuleEngine().fire(rule, facts)` 执行。组合规则可使用 `AllRules`、`AnyRules`、`CompositeRule`。

**当前依赖了什么**：无生产依赖，测试只依赖 JUnit。

**需要注意什么**：当前 `DefaultRuleEngine` 在 rule 为 null 时使用 `System.err`，审查时建议改成 `ILogger` 或明确异常。复杂规则、持久化规则和表达式解析不要直接堆进 core，应拆成独立扩展。
