# statemachine

## 模块定位

`infrastructure-component-statemachine` 是独立状态机组件，提供状态、事件、上下文、transition、action、condition、builder DSL 和运行时执行能力。

## 核心能力

- `StateMachine`、`StateMachineFactory`。
- `State`、`Transition`、`StateContext`。
- `Action`、`Condition`、`ActionWrapper`、`ActionWrapperChain`。
- builder：`StateMachineBuilderFactory`、`StateMachineBuilder`、`ExternalTransitionBuilder`、`InternalTransitionBuilder` 等。
- impl：`StateMachineImpl`、`TransitionImpl`、`StateImpl`、`EventTransitions`。
- 可视化和调试：`Visitor`、`PlantUMLVisitor`、`SysOutVisitor`、`Debugger`。
- `IStateMachineAbility`：能力 mixin。

## 依赖边界

- 不依赖 Quarkus、CDI、数据库、MQ。
- workflow 组件可以依赖状态机，状态机不依赖 workflow。
- 状态、事件、上下文均使用泛型，避免绑定具体业务模型。

## 对外 API

- `StateMachineBuilderFactory.create(S, E, C)` 或当前 `create()` 泛型 builder 入口。
- builder 的 `state(...)`、transition 构造和 `build()`。
- `StateMachine#fireEvent(...)` 风格的事件触发接口。

## 典型使用场景

- 业务实体状态流转。
- 审批、订单、任务、工作流等场景的状态控制。
- 生成 PlantUML 辅助审查状态图。

## 维护事项

- builder DSL 的向后兼容性很重要，避免频繁破坏业务调用。
- 状态迁移失败异常 `TransitionFailException` 要保持语义清楚。
- 调试输出不应污染生产日志，必要时接入日志组件。

## 测试验收

- `./gradlew :infrastructure-component-statemachine:test` 通过。
- 外部迁移、内部迁移、并行迁移、条件失败、action chain 有测试覆盖。
- 源码中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：提供通用状态流转模型，解决审批、订单、任务、工作流等场景中状态、事件、条件和动作分散实现的问题。

**如何使用**：通过 `StateMachineBuilderFactory.create()` 创建 builder，定义 state、external/internal transition、condition 和 action，最后 `build()` 得到 `StateMachine`，运行时传入事件和 `StateContext` 触发流转。

**当前依赖了什么**：无生产依赖，测试只依赖 JUnit。

**需要注意什么**：状态机是 workflow 的下层能力，不能反向依赖 workflow。审查时重点看 builder DSL 的兼容性、失败回调语义、action wrapper 执行顺序和并行 transition 的边界。
