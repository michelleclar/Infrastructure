# qdrant-grpc

## 模块定位

`infrastructure-component-qdrant-grpc` 提供 Qdrant gRPC client、points/collections client 和大量 request factory。当前模块同时包含 Quarkus provider，因此不是纯粹的独立 client 模块。

## 核心能力

- `QdrantGrpcClient`：聚合 points 和 collections gRPC client。
- `PointsGrpcClient`、`CollectionsGrpcClient`。
- `IQdrantAbility`：暴露 Qdrant client、points、collections 的能力 mixin。
- factory 包：构造 vector、point id、query、condition、payload selector、shard key 等 protobuf 请求对象。
- protobuf 代码生成：使用 `dil/protos` 和 Vert.x gRPC protoc 插件。

## 依赖边界

- client/factory 层可以依赖 Qdrant protobuf、Vert.x gRPC 和 protobuf 类型。
- `QdrantGrpcClientProvider` 依赖 Quarkus Arc、CDI、MicroProfile Config，应迁到 Quarkus adapter。
- 如果本模块继续作为独立组件发布，应移除 Quarkus provider 或拆分为 `qdrant-grpc` core 与 `quarkus:qdrant` adapter。

## 对外 API

- `QdrantGrpcClient#getPointsGrpcClient()`。
- `QdrantGrpcClient#getCollectionsGrpcClient()`。
- `IQdrantAbility#getPoints()`。
- `IQdrantAbility#getCollections()`。
- `ConditionFactory`、`VectorFactory`、`QueryFactory` 等 request factory。

## 典型使用场景

- 向 Qdrant 写入和查询向量 points。
- 管理 Qdrant collections。
- 构造复杂 filter、vector、query、payload selector。
- Quarkus 项目通过 provider 自动注入默认 client。

## 维护事项

- 修正 `clents` 包名拼写时需要兼容迁移。
- Qdrant proto 更新后要验证所有 factory 与生成类型兼容。
- provider 中的配置前缀 `quarkus.qdrant.*` 应仅存在于 adapter。

## 测试验收

- `./gradlew :infrastructure-component-qdrant-grpc:test` 通过。
- protobuf 生成任务稳定运行。
- 如果拆分 adapter，core client 源码中没有 Quarkus、CDI、MicroProfile Config import。
- factory 方法有单元测试覆盖常用请求构造。

## 使用与依赖补充

**为了解决什么**：封装 Qdrant gRPC points/collections 操作和复杂 protobuf 请求构造，降低向量库调用成本。

**如何使用**：当前 Quarkus 场景可配置 `quarkus.qdrant.host`、`quarkus.qdrant.port`，由 `QdrantGrpcClientProvider` 生产 client；直接使用时手动创建 Vert.x 和 `SocketAddress` 后实例化 `QdrantGrpcClient`。构造请求时使用 `VectorFactory`、`PointStructFactory`、`ConditionFactory`、`QueryFactory` 等。

**当前依赖了什么**：`api(libs.quarkus.grpc)`、`implementation(project(":infrastructure-component-utils"))`，测试依赖 JUnit 和 `io.vertx:vertx-junit5`，并使用 protobuf 插件。

**需要注意什么**：当前模块混入 Quarkus provider，不是纯 client。审查时要把 `QdrantGrpcClientProvider` 迁到 adapter，并检查 factory 是否泄漏过多生成类型给业务层。
