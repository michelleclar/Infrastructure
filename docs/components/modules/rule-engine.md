# rule-engine

`infra-component-rule-engine` 是 QLExpress 4.1.3 的薄封装，不是业务规则编排引擎。包 `org.carl.infra.ruleengine` 只有三个顶层类：

- `ScriptValidator.check(script)`：拒绝空白/null，仅语法检查，保留 QLExpress 错误详情，不执行或验证业务。
- `ConditionCompiler.compile(Map<String, ?>)`：应用绑定 JSON；输出嵌套 `CompiledCondition(script, parameters)`。
- `QLExpressExecutor.execute(script, facts)` / `execute(compiled, facts)`：返回原始结果；构造器可传正数超时毫秒，默认 1000。

条件组 `{combinator:and|or,rules:[...]}` 支持嵌套；叶 `{field:user.level,operator:==,value:VIP}` 支持 `== != > >= < <= in`。严格拒绝未知键、空组、混合节点、缺失 value（显式 null 合法）、非法路径和超过 32 层的树。值全部参数化，条件完整加括号，`__condition_` 保留命名空间不得作为字段根或编译执行的 facts 顶层键，合并不覆盖同名 facts。

每个 executor 复用一个默认隔离 runner，启用解析缓存、正数超时和 `polluteUserContext(false)`；每次执行浅复制顶层 Map，**不保证嵌套数据深层只读**。并发调用使用独立 facts，执行期间嵌套值应稳定。语法成功不保证运行成功，比较和运行错误遵循 QLExpress。协作式超时不是硬资源沙箱。

依赖 QLExpress；不引入 Jackson 或额外 JSON 库，不用 DTO 自动绑定/反射路径读取，不提供 native 注册；**Map 路径已通过 native 实测**（macOS arm64、Quarkus 3.19.3、GraalVM CE 21.0.2，详情见模块 README）。

破坏性变更：移除旧 `RuleEngine` / `Rule` / `RuleSet` / `Facts` / `IRuleEngineAbility`、lambda DSL、动作、监听器、元数据和轨迹。应用负责 JSON 解析、业务编排、权限、存储和副作用。详细 API 与迁移见 `infra-component-rule-engine/README.MD`。

验收：`./gradlew :infra-component-rule-engine:clean :infra-component-rule-engine:test :infra-component-rule-engine:build`。公共文档随 main 演进，以固定 4.1.3 的本地测试为准。
