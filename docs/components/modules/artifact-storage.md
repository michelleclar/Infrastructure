# artifact-storage

## 模块定位

`infrastructure-component-artifact-storage` 是应用中立 artifact 存储组件，提供存储接口、写入请求、元数据模型和本地文件 provider。

## 核心能力

- `ArtifactStorage`：存储抽象接口。
- `ArtifactWriteRequest`：写入请求，包含 key、内容和 content type。
- `ArtifactMetadata`：key、content type、字节数、存储路径和创建时间。
- `LocalArtifactStorage`：本地文件实现。

## 依赖边界

- 不依赖 Quarkus、CDI、JAX-RS、MicroProfile Config。
- 不依赖 ER Tool artifact、audit 或业务错误码。
- 后续 MinIO/OSS 实现应继续通过 `ArtifactStorage` 暴露。

## 测试验收

- `./gradlew :infrastructure-component-artifact-storage:test` 通过。
- 本地 provider 防止路径穿越。
- 保存内容时创建父目录并写入 metadata sidecar。
- metadata lookup 返回 content type 和 byte size。
