# rule-engine

`infra-component-rule-engine` 是 QLExpress 4.1.3 的薄封装，不是业务规则编排引擎。包 `org.carl.infra.ruleengine` 的公共入口如下：

- `ConditionCompiler.compile(Map<String, ?>)`：应用绑定 JSON；输出嵌套 `CompiledCondition(script, parameters)`。
- `ExpressionOptions(long timeoutMillis, boolean precise)`：不可变配置，默认 `(1000, false)`，拒绝非正超时。
- `ExpressionFactory.getDefault()` / `get(options)`：按配置值相等缓存并返回 `ExpressionExecutor`，不暴露原始 runner/厂商配置；`ExpressionExecutor` 是公共 final 具体封装类，也可无参或传 `long timeoutMillis` 创建独立实例（precise=false），配置构造器仅包内可见。
- `ExpressionExecutor.check(script)`：拒绝空白/null，仅语法检查，保留 QLExpress 错误详情，不执行或验证业务。`execute(script, facts)` / `execute(compiled, facts)` 返回原始结果，不自动先 check。

```java
ExpressionExecutor executor = ExpressionFactory.get(new ExpressionOptions(2000L, true));
var compiler = new ConditionCompiler(executor); // 无参构造使用工厂默认 executor
var compiled = compiler.compile(Map.of("field", "age", "operator", ">=", "value", 18));
Object result = executor.execute(compiled, Map.of("age", 25));
```

工厂使用 `ConcurrentHashMap.computeIfAbsent`，缓存无界、无淘汰，只适合有限的应用自有配置，不能接收任意公网配置输入；runner 的解析缓存同样无界，应控制脚本种类。

条件组 `{combinator:and|or,rules:[...]}` 支持嵌套；叶 `{field:user.level,operator:==,value:VIP}` 支持 `== != > >= < <= in`。严格拒绝未知键、空组、混合节点、缺失 value（显式 null 合法）、非法路径和超过 32 层的树。值全部参数化，条件完整加括号，`__condition_` 保留命名空间不得作为字段根或编译执行的 facts 顶层键，合并不覆盖同名 facts。

每个 executor 复用一个默认隔离 runner，启用解析缓存、正数超时和 `polluteUserContext(false)`；每次执行浅复制顶层 Map，**不保证嵌套数据深层只读**。并发调用使用独立 facts，执行期间嵌套值应稳定。语法成功不保证运行成功，比较和运行错误遵循 QLExpress。没有新增异常层次，QLExpress 异常原样传播；DSL/异常仍与厂商绑定，不承诺透明切换引擎或迁移脚本。协作式超时不是硬资源沙箱。

依赖 QLExpress；不引入 Jackson 或额外 JSON 库，不用 DTO 自动绑定/反射路径读取，不提供 native 注册；历史 native 实测仅针对旧 API 的 Map 路径（环境见模块 README），**当前工厂 API 尚未验证 native**。

破坏性变更：移除旧 `RuleEngine` / `Rule` / `RuleSet` / `Facts` / `IRuleEngineAbility`、lambda DSL、动作、监听器、元数据和轨迹。应用负责 JSON 解析、业务编排、权限、存储和副作用。详细 API 与迁移见 `infra-component-rule-engine/README.MD`。

验收：`./gradlew :infra-component-rule-engine:test :infra-component-rule-engine:build`。公共文档随 main 演进，以固定 4.1.3 的本地测试为准。
