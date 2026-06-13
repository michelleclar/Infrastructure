# infrastructure-component-statemachine

> 轻量级有限状态机（FSM）库，源自 [COLA](https://github.com/alibaba/COLA)。以流式 DSL 声明状态、事件、条件（`Condition`）和动作（`Action`），构建**无状态**、线程安全的状态机实例，由 `StateMachineFactory` 统一注册与查找。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-statemachine"))
}
```

无任何外部运行时依赖；仅 JDK 21。

---

## 核心概念

| 概念 | 类 / 接口 | 说明 |
|------|-----------|------|
| 状态机 | `StateMachine<S,E,C>` | 顶级接口；无状态、可多线程共享 |
| 构建器工厂 | `StateMachineBuilderFactory` | 唯一入口：`StateMachineBuilderFactory.create()` |
| 构建器 | `StateMachineBuilder<S,E,C>` | 声明转换规则，调用 `.build(machineId)` 注册实例 |
| 状态注册表 | `StateMachineFactory` | `register` / `get` / `unregister` / `contains` |
| 外部转换 | `ExternalTransitionBuilder` | 触发后**离开**当前状态，产生状态变更 |
| 多源外部转换 | `ExternalTransitionsBuilder` | 多个源状态 → 同一目标状态 |
| 并行转换 | `ExternalParallelTransitionBuilder` | 一个源状态 → 多个目标状态（`fireParallelEvent`） |
| 内部转换 | `InternalTransitionBuilder` | 触发后**不离开**当前状态，仅执行动作 |
| 条件守卫 | `Condition<C>` | `boolean isSatisfied(C context)` |
| 转换动作 | `Action<S,E,C>` | `void execute(S from, S to, E event, C context)` |
| 动作包装器 | `ActionWrapper<S,E,C>` | 装饰器接口；`ActionWrapperChain.chain(base, wrappers...)` 组合多层 |
| 失败回调 | `FailCallback<S,E,C>` | 无匹配转换时调用；默认实现 `NumbFailCallback`（静默忽略） |
| 异常抛出 | `AlertFailCallback<S,E,C>` | 失败时抛 `TransitionFailException` |
| PlantUML | `StateMachine.generatePlantUML()` | 导出状态图 DSL 字符串 |

泛型约定：`S` = 状态类型（通常为 `enum`），`E` = 事件类型（通常为 `enum`），`C` = 业务上下文类型。

---

## 定义状态机

以内容发布流程为例（草稿 → 审核 → 发布）：

```java
// 1. 定义状态、事件、业务上下文
enum Status  { DRAFT, SUBMITTED, PROCESSING, PUBLISHED, REJECTED, UNPUBLISHED }
enum Event   { SUBMIT, APPROVE, REJECT, PUBLISH, REVISE, WITHDRAW, UNPUBLISH }

class PublishCtx {
    String authorId;
    String reviewerId;
    String rejectReason;
    // …业务字段…

    boolean isContentReady()       { return /* … */; }
    boolean isApprovalConditionMet() { return authorId != null && rejectReason == null; }
    boolean canPublish()           { return isContentReady() && rejectReason == null; }
}

// 2. 创建构建器
StateMachineBuilder<Status, Event, PublishCtx> builder = StateMachineBuilderFactory.create();

// 外部转换：DRAFT --[SUBMIT]--> SUBMITTED（需满足 isContentReady 条件）
builder.externalTransition()
        .from(Status.DRAFT)
        .to(Status.SUBMITTED)
        .on(Event.SUBMIT)
        .when(ctx -> ctx.isContentReady())
        .perform((from, to, event, ctx) ->
                System.out.println("提交审核: " + ctx.authorId));

// 外部转换：SUBMITTED --[APPROVE]--> PROCESSING
builder.externalTransition()
        .from(Status.SUBMITTED)
        .to(Status.PROCESSING)
        .on(Event.APPROVE)
        .when(ctx -> ctx.isApprovalConditionMet())
        .perform((from, to, event, ctx) ->
                System.out.println("审核通过"));

// 多源转换：SUBMITTED / REJECTED 均可触发 REVISE → DRAFT
builder.externalTransitions()
        .fromAmong(Status.SUBMITTED, Status.REJECTED)
        .to(Status.DRAFT)
        .on(Event.REVISE)
        .when(ctx -> ctx.authorId != null)
        .perform((from, to, event, ctx) -> {});

// 内部转换：在 PROCESSING 状态内处理内部事件，不改变状态
builder.internalTransition()
        .within(Status.PROCESSING)
        .on(Event.SUBMIT)                       // 复用同一事件；条件决定走哪条分支
        .when(ctx -> !ctx.isContentReady())
        .perform((from, to, event, ctx) ->
                System.out.println("内容未就绪，等待"));

// 失败时抛异常（默认静默忽略）
builder.setFailCallback(new AlertFailCallback<>());

// 3. 构建并注册，machineId 全局唯一
StateMachine<Status, Event, PublishCtx> machine = builder.build("CONTENT_PUBLISHING");
```

> `build(machineId)` 内部调用 `StateMachineFactory.register()`，同一 `machineId` 重复注册会抛 `StateMachineException`。

---

## 触发事件 / 查询状态

```java
// 从注册表获取（跨类、跨线程安全复用）
StateMachine<Status, Event, PublishCtx> machine =
        StateMachineFactory.get("CONTENT_PUBLISHING");

PublishCtx ctx = new PublishCtx();
ctx.authorId  = "alice";
ctx.reviewerId = "bob";

// 触发单一转换，返回目标状态
Status next = machine.fireEvent(Status.DRAFT, Event.SUBMIT, ctx);
// next == Status.SUBMITTED

// 预检：不执行动作，只判断转换是否存在
boolean canFire = machine.verify(Status.DRAFT, Event.SUBMIT);

// 并行转换：一个事件落到多个目标状态
List<Status> targets = machine.fireParallelEvent(Status.DRAFT, Event.SUBMIT, ctx);
```

---

## 条件分支（Choice）

同一 `(sourceState, event)` 注册多条转换，由 `Condition` 决定走哪条（**按注册顺序**，第一个满足的分支生效）：

```java
// EVENT1 在 STATE1：条件1 → 内部转换（不变状态），条件2 → STATE2，条件3 → STATE3
builder.internalTransition()
        .within(States.STATE1).on(Events.EVENT1)
        .when(ctx -> "1".equals(ctx.flag)).perform(doAction());

builder.externalTransition()
        .from(States.STATE1).to(States.STATE2).on(Events.EVENT1)
        .when(ctx -> "2".equals(ctx.flag)).perform(doAction());

builder.externalTransition()
        .from(States.STATE1).to(States.STATE3).on(Events.EVENT1)
        .when(ctx -> "3".equals(ctx.flag)).perform(doAction());
```

---

## 动作链（ActionWrapperChain）

用 `ActionWrapper` 在核心动作外叠加日志、监控等横切关注点：

```java
Action<S, E, C> base = (from, to, event, ctx) -> { /* 核心业务 */ };

ActionWrapper<S, E, C> logging = next -> (from, to, event, ctx) -> {
    System.out.println("before: " + event);
    next.execute(from, to, event, ctx);
    System.out.println("after: " + event);
};

Action<S, E, C> wrapped = ActionWrapperChain.chain(base, logging);
// 将 wrapped 传给 .perform(wrapped)
```

---

## 可视化

```java
// 控制台打印状态机结构
machine.showStateMachine();

// 导出 PlantUML 字符串，粘贴到 https://www.plantuml.com/plantuml 渲染
String uml = machine.generatePlantUML();
System.out.println(uml);
```

---

## Ability 模式（推荐集成方式）

实现 `IStateMachineAbility<S,E,C>` 而不是直接注入 `StateMachine`，保持依赖边界清晰：

```java
public class ContentPublishingService
        implements IStateMachineAbility<Status, Event, PublishCtx> {

    private final StateMachine<Status, Event, PublishCtx> sm =
            StateMachineFactory.get("CONTENT_PUBLISHING");

    @Override
    public StateMachine<Status, Event, PublishCtx> getStateMachine() {
        return sm;
    }

    public Status submit(String entityId, PublishCtx ctx) {
        return getStateMachine().fireEvent(Status.DRAFT, Event.SUBMIT, ctx);
    }
}
```

---

## 注意事项

| 事项 | 说明 |
|------|------|
| 无状态设计 | `StateMachine` 实例本身不保存"当前状态"，当前状态由**调用方**传入 `fireEvent(sourceState, ...)`，需自行持久化 |
| machineId 全局唯一 | 同一 JVM 内重复 `build()` 相同 `machineId` 抛 `StateMachineException`；测试后需 `StateMachineFactory.unregister(machineId)` 清理 |
| 条件匹配顺序 | 多条件分支按**注册顺序**匹配，无条件的转换兜底（注册在最后） |
| 失败行为 | 默认 `NumbFailCallback` 静默返回 `sourceState`；生产环境推荐注册 `AlertFailCallback` 或自定义 `FailCallback` |
| 线程安全 | `StateMachineImpl` 无可变字段，构建完成后多线程安全共享 |
| 并行转换状态管理 | `fireParallelEvent` 返回 `List<S>`，需业务方自行决定如何合并多目标状态 |
