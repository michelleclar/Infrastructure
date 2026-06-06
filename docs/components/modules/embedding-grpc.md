# embedding-grpc

## 模块定位

`infrastructure-component-embedding-grpc` 提供 embedding 服务的 Vert.x gRPC client、能力接口和 Quarkus provider。它当前是“gRPC client + Quarkus 装配”混合模块。

## 核心能力

- `EmbeddingGrpcClient`：封装 text-to-vector、face-to-vector 等 gRPC 调用。
- `IEmbeddingAbility`：暴露 embedding client 的能力 mixin。
- `EmbeddingGrpcClientProvider`：通过 Quarkus 配置创建默认 client。
- protobuf 代码生成：使用 `dil/protos` 和 Vert.x gRPC protoc 插件。

## 依赖边界

- client 层可以依赖 Vert.x gRPC 和生成的 protobuf 类型。
- Quarkus provider 依赖 `io.quarkus.arc.*`、CDI、MicroProfile Config，应迁入 Quarkus adapter 或独立 adapter 包。
- 如果目标是纯独立组件，`EmbeddingGrpcClientProvider` 不应留在 core 模块。

## 对外 API

- `IEmbeddingAbility#getEmbeddingClient()`。
- `IEmbeddingAbility#textToVector(...)`。
- `IEmbeddingAbility#faceToVector(...)`。
- `EmbeddingGrpcClient` 的底层 gRPC 调用方法。

## 典型使用场景

- 文本向量化。
- 图片或人脸向量化。
- 业务模块通过 ability 接口调用 embedding 服务。

## 维护事项

- 修正 `clents` 包名拼写时要做兼容迁移，避免破坏调用方。
- protobuf 更新后需要确认生成源码与现有 factory/client API 兼容。
- 建议后续拆成 `embedding-grpc` core client 与 `quarkus:embedding` provider。

## 测试验收

- `./gradlew :infrastructure-component-embedding-grpc:test` 通过。
- protobuf 生成任务能稳定运行。
- 若拆分 provider，core client 源码中没有 Quarkus、CDI、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：封装外部 embedding gRPC 服务调用，让业务以 Java client 方式获取文本或人脸向量。

**如何使用**：当前 Quarkus 场景可配置 `quarkus.embedding.host`、`quarkus.embedding.port`，由 `EmbeddingGrpcClientProvider` 生产 `EmbeddingGrpcClient`；直接使用时可手动创建 Vert.x `GrpcClient` 和 `SocketAddress` 后实例化 `EmbeddingGrpcClient`。

**当前依赖了什么**：`api(libs.quarkus.grpc)`，测试依赖 JUnit 和 `io.vertx:vertx-junit5`，并通过 protobuf 插件从 `dil/protos` 生成代码。

**需要注意什么**：该模块目前混入 Quarkus provider，不是纯独立组件。审查时应把 generated proto、Vert.x gRPC client 留在 core client，把配置读取和 CDI producer 迁到 Quarkus adapter。
