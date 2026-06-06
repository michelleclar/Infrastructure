# pdp

## 模块定位

`infrastructure-component-pdp` 是策略决策点组件，提供 Policy Decision Point 的最小抽象和默认实现。它适合被 authorization 或业务权限系统复用。

## 核心能力

- `Pdp`：策略评估入口。
- `Policy`：单条策略接口。
- `PolicyRequest`：策略评估请求。
- `PolicyDecision`：决策结果。
- `DefaultPdp`：按策略列表执行评估，DENY 优先，至少一个 PERMIT 才返回 PERMIT，否则 DENY。
- `IPdpAbility`：能力 mixin。

## 依赖边界

- 不依赖 Quarkus、CDI、JAX-RS、数据库或外部 PDP 服务。
- 可被 authorization core 使用，但不应反向依赖 authorization adapter。
- 外部 PDP/OpenFGA/OPA 集成应作为实现模块或 adapter，不放入当前最小 core。

## 对外 API

- `Pdp#evaluate(PolicyRequest request)`。
- `Policy#evaluate(PolicyRequest request)`。
- `DefaultPdp(List<Policy> policies)`。

## 典型使用场景

- 本地策略评估。
- 权限系统中组合多条策略，形成 deny override 的决策结果。
- 测试权限规则时用轻量 PDP 替代外部授权服务。

## 维护事项

- 决策合并规则是核心契约，修改 DENY/PERMIT 优先级必须谨慎。
- `PolicyRequest` 字段扩展要保持向后兼容。
- 如果引入更多决策结果，如 NOT_APPLICABLE，应明确默认合并语义。

## 测试验收

- `./gradlew :infrastructure-component-pdp:test` 通过。
- DENY 优先、无策略、全部 DENY、至少一个 PERMIT 等分支有测试。
- 模块源码中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：提供一个本地策略决策点，用统一规则合并多个 `Policy` 的判断结果，为 authorization 或业务权限模块提供决策基础。

**如何使用**：实现若干 `Policy`，创建 `new DefaultPdp(policies)`，传入 `PolicyRequest` 调用 `evaluate(...)`。默认语义是任一策略 DENY 则 DENY，否则有 PERMIT 则 PERMIT，没有 PERMIT 则 DENY。

**当前依赖了什么**：无生产依赖，测试只依赖 JUnit。

**需要注意什么**：DENY 优先是安全敏感契约，不能随意改。审查时要确认 `PolicyRequest` 是否足够表达 subject/action/resource/context，不够再扩展模型。
