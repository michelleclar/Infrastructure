# discover

## 当前定位

`infrastructure-component-quarkus:discover` 当前核心代码是 `ServiceLifecycle`，通过 Quarkus 启停事件、Vert.x Consul client 和配置项完成服务注册与注销。它本质上是 Quarkus 生命周期 adapter。

## 是否需要独立 core

暂不需要强拆。当前没有明显的跨运行时服务发现 core。后续如需要支持非 Quarkus 应用，可抽出服务注册 API。

## 建议独立模块名

可选：`infrastructure-component-discover-api`

仅在需要复用服务注册模型或支持多种注册中心时创建。

## Quarkus adapter 应保留内容

- `StartupEvent`、`ShutdownEvent` 观察者。
- Vert.x `ConsulClient` 创建和关闭。
- `ConfigProperty` 读取。
- Quarkus 应用端口、服务名、健康检查地址的组装。

## 需要迁出的核心能力

- 可选迁出服务实例描述模型，例如 service id、name、host、port、tags、health check。
- 可选迁出服务注册接口，例如 register、deregister、heartbeat。
- 与 Consul 无关的注册参数校验。

## 依赖边界

- adapter 可以依赖 Quarkus runtime、MicroProfile Config、Vert.x Consul client。
- 如果创建 discover api，api 层不得依赖 Quarkus、Vert.x 或 Consul client 类型。
- Consul 具体实现可留在 Quarkus adapter，或拆成独立 `discover-consul` 实现模块。

## 拆分任务清单

- 保持当前 discover 为 Quarkus adapter。
- 给 `ServiceLifecycle` 补充职责说明，避免继续加入通用发现逻辑。
- 如果后续出现非 Quarkus 使用场景，再创建 discover api。
- 抽出服务注册参数模型时，先保证不泄漏 Vert.x/Quarkus 类型。

## 验收标准

- `./gradlew :infrastructure-component-quarkus:discover:test` 通过。
- discover 模块中的新增代码仍然只处理 Quarkus 生命周期和 Consul adapter。
- 若新增 api 模块，其源码中没有 Quarkus、Vert.x、MicroProfile Config import。

## 模块审查补充

**解决的问题**：在 Quarkus 应用启动时把服务注册到 Consul，在关闭时注销，解决服务发现和健康检查接入问题。

**如何使用**：依赖 `infrastructure-component-quarkus:discover`，配置 `consul.host`、`consul.port`、`quarkus.application.name`、`quarkus.http.port`。应用启动后 `ServiceLifecycle` 监听 `StartupEvent` 完成注册，关闭时监听 `ShutdownEvent` 注销。

**当前依赖**：`implementation(libs.bundles.discover)`，源码使用 Quarkus lifecycle、Vert.x Consul client、MicroProfile Config。

**需要注意**：当前模块只有 adapter 性质，不要继续加入通用服务发现策略。审查时重点看注册服务名、端口、健康检查地址是否满足部署环境；如要支持非 Quarkus 应用，再抽 `discover-api`。
