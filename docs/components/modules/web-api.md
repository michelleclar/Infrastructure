# web-api

## 模块定位

`infrastructure-component-web-api` 是纯 Java Web API 支撑组件，提供响应信封、请求 ID、验证错误、业务异常、404 异常、兜底异常映射和可替换的响应适配接口。

## 核心能力

- `ResponseEnvelope`：默认响应信封。
- `WebRequestContext`：请求 ID 上下文。
- `ValidationError`：字段验证错误模型。
- `WebError`：状态、错误码、消息、场景和验证错误集合。
- `BusinessWebException`、`ValidationWebException`、`NotFoundWebException`。
- `WebExceptionMapper`：将异常转换成 `MappedWebResponse`。
- `ResponseEnvelopeAdapter`：让应用保持既有响应结构。

## 依赖边界

- 不依赖 Quarkus、CDI、JAX-RS、Vert.x 或 MicroProfile Config。
- 不依赖 ER Tool DTO、异常或业务错误码。
- Quarkus exception mapper 应作为 adapter 使用这些纯 Java API。

## 测试验收

- `./gradlew :infrastructure-component-web-api:test` 通过。
- 覆盖业务错误、验证错误、404 和 fallback 500。
- 覆盖自定义 `ResponseEnvelopeAdapter`，证明应用可保持既有响应结构。
