# broadcast

## 当前定位

`infrastructure-component-quarkus:broadcast` 主要围绕 Vert.x EventBus、Mutiny `Uni` 和 CDI provider 暴露广播能力。该能力目前与 Quarkus/Vert.x 运行时绑定较深，暂按 adapter 保留。

## 是否需要独立 core

暂不强拆。只建议抽出与 Vert.x 无关的事件模型、常量或接口，避免业务代码直接依赖 Quarkus adapter。

## 建议独立模块名

可选：`infrastructure-component-broadcast-api`

仅在出现跨运行时广播实现时再创建，例如 Redis pub/sub、Pulsar topic 或内存广播。

## Quarkus adapter 应保留内容

- `BroadcastContextProvider` CDI producer。
- Vert.x `EventBus` 注入和发送/订阅实现。
- `BroadcastRegistry` 中基于 `MessageConsumer` 的注册管理。
- `@IfBuildProperty` 开关和 `BroadcastService` Bean 注册。

## 需要迁出的核心能力

- 与 Vert.x 无关的广播接口，例如发布、请求、订阅、取消订阅的最小契约。
- 事件地址、topic 或消息 envelope 的通用模型。
- 不依赖 `Uni` 的同步或 `CompletionStage` 形式接口，如果未来需要脱离 Mutiny。

## 依赖边界

- adapter 可以依赖 Vert.x EventBus、Mutiny、Quarkus Arc。
- core/api 层不得暴露 `EventBus`、`MessageConsumer`、`RoutingContext` 等 Vert.x 类型。
- 业务模块如只需要广播能力，应依赖抽象接口，不直接依赖 Quarkus broadcast 的实现类。

## 拆分任务清单

- 先标记当前 Quarkus broadcast 为 Vert.x adapter。
- 识别 `IBroadcastOperations`、`IBroadcastAbility` 中是否有必要暴露 Vert.x 类型。
- 如果业务代码需要跨运行时复用，创建 `infrastructure-component-broadcast-api`。
- 将事件模型和最小广播接口迁入 api 模块。
- 保留 Vert.x 具体实现和 CDI provider 在 Quarkus adapter。

## 验收标准

- 若创建 api 模块，`./gradlew :infrastructure-component-broadcast-api:test` 通过。
- `./gradlew :infrastructure-component-quarkus:broadcast:test` 通过。
- api 模块不包含 Vert.x、Mutiny、Quarkus、CDI import。
- Quarkus broadcast 对外暴露的核心接口不强迫调用方导入 Vert.x 类型。

## 模块审查补充

**解决的问题**：在 Quarkus 服务内部提供基于 Vert.x EventBus 的广播、请求和订阅能力，解决模块之间轻量事件分发问题。

**如何使用**：Quarkus 应用依赖 `infrastructure-component-quarkus:broadcast`，配置 `quarkus.plugins.broadcast.enable=true` 后注入 `IBroadcastAbility` 或 `IBroadcastOperations`，通过 EventBus 地址发送、请求或订阅消息。

**当前依赖**：`implementation(libs.quarkus.vertx)`，测试侧依赖 `infrastructure-component-dto`。

**需要注意**：该模块当前对 Vert.x 和 Mutiny 类型暴露较多，属于强 adapter。审查时要区分“事件模型和广播抽象”与“EventBus 实现”。如果调用方被迫导入 `EventBus`、`MessageConsumer`、`Uni`，说明抽象边界还不够干净。
