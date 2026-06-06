# http

## 模块定位

`infrastructure-component-http` 是独立 HTTP client 组件，提供统一请求/响应模型、拦截器上下文和 Apache async HTTP 实现。

## 核心能力

- `HttpClient`：基于 `CompletionStage<HttpResponse>` 的异步执行接口。
- `HttpClientFactory`：创建默认 Apache async client。
- `HttpClientOptions`：client 配置。
- `HttpRequest`、`HttpResponse`：请求和响应模型。
- `HttpInterceptor` 以及 request/response/error/completion context。
- `ApacheAsyncHttpClient`：Apache HTTP 实现。

## 依赖边界

- 可以依赖 Apache HTTP client 相关库。
- 不依赖 Quarkus、CDI、JAX-RS 或 Vert.x Web。
- 上层如 Quarkus web 需要 HTTP client，应依赖该模块，不应重复实现。

## 对外 API

- `HttpClientFactory.create()`。
- `HttpClientFactory.create(HttpClientOptions options)`。
- `HttpClient#execute(HttpRequest request)`。
- `HttpClient#close()`。

## 典型使用场景

- 基础设施组件调用外部 HTTP 服务。
- 业务代码需要统一拦截器、错误处理和异步响应模型。
- 测试中通过 `HttpClient` 接口替换真实 HTTP 调用。

## 维护事项

- 拦截器执行顺序和异常传播规则要保持稳定。
- 新增实现时应通过 `HttpClient` 接口暴露，不让调用方绑定 Apache 实现。
- 超时、连接池、重试等策略应进入 `HttpClientOptions`，避免散落在调用端。

## 测试验收

- `./gradlew :infrastructure-component-http:test` 通过。
- 请求构造、响应解析、拦截器链和异常路径有测试覆盖。
- 模块源码中没有 Quarkus、CDI、JAX-RS import。

## 使用与依赖补充

**为了解决什么**：提供不绑定 Quarkus 的统一 HTTP 调用能力，解决外部服务调用、拦截器、错误上下文和异步响应模型重复实现的问题。

**如何使用**：通过 `HttpClientFactory.create()` 创建默认 client，构造 `HttpRequest` 后调用 `client.execute(request)`，返回 `CompletionStage<HttpResponse>`。需要连接池、超时或拦截器时使用 `HttpClientOptions`。

**当前依赖了什么**：`implementation(libs.bundles.http)`，底层实现是 `ApacheAsyncHttpClient`。

**需要注意什么**：调用方应依赖 `HttpClient` 接口，不依赖 Apache 实现。审查时关注 close 释放资源、拦截器异常传播、超时默认值和响应 body 消费方式。
