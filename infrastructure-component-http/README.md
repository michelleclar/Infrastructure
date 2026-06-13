# infrastructure-component-http

基于 Apache HttpComponents 5（异步 NIO）封装的轻量 HTTP 客户端。提供统一的 `HttpClient` 接口、流式请求构建、6 阶段拦截器链，以及开箱即用的连接池管理。无框架依赖，可在任何 Java 21 项目中直接使用。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-http"))
}
```

底层实现为 `org.apache.httpcomponents.client5:httpclient5:5.4.1`（通过版本目录 `libs.bundles.http` 传递引入），无需额外声明。

---

## 核心 API

| 类 / 接口 | 说明 |
|---|---|
| `HttpClient` | 顶层接口，`execute(HttpRequest) → CompletionStage<HttpResponse>`，实现 `AutoCloseable` |
| `HttpClientFactory` | 静态工厂，`create()` / `create(HttpClientOptions)` 返回 `HttpClient` 实例 |
| `HttpRequest` | 不可变请求对象；通过内部 `Builder` 构造 |
| `HttpRequest.Builder` | 流式请求构建器，提供 `get/post/put/delete` 快捷静态方法 |
| `HttpResponse` | 不可变响应对象，含状态码、响应头、字节体及 `getBodyAsString()` 便捷方法 |
| `HttpClientOptions` | 客户端配置值对象；通过内部 `Builder` 构造，所有字段均有默认值 |
| `HttpClientOptions.Builder` | 配置构建器，支持超时、连接池大小、拦截器链 |
| `HttpInterceptor` | 拦截器接口，6 个 `default` 回调方法，按需覆盖 |
| `HttpClientException` | 统一运行时异常，包裹网络错误、超时、连接被拒等失败场景 |

### 拦截器回调顺序

| 方法 | 触发时机 |
|---|---|
| `beforeRequest(HttpRequestContext)` | 请求对象构建完毕、发送前 |
| `beforeSend(HttpRequestContext)` | 底层 I/O 发送前 |
| `afterResponseHeaders(HttpResponseContext)` | 响应头接收完毕 |
| `afterResponseBody(HttpResponseContext)` | 响应体接收完毕（成功路径） |
| `onError(HttpErrorContext)` | 传输层错误或超时（失败路径） |
| `afterCompletion(HttpCompletionContext)` | 请求生命周期结束，无论成功或失败均触发 |

成功路径顺序：`beforeRequest → beforeSend → afterResponseHeaders → afterResponseBody → afterCompletion`  
失败路径顺序：`beforeRequest → beforeSend → onError → afterCompletion`

---

## 编程式配置（`HttpClientOptions`）

本模块**不读取任何配置文件**，所有配置通过 `HttpClientOptions.Builder` 在代码中指定：

| Builder 方法 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `connectTimeout(Duration)` | `Duration` | `Duration.ofSeconds(10)` | TCP 连接建立超时 |
| `responseTimeout(Duration)` | `Duration` | `Duration.ofSeconds(30)` | 等待响应超时（客户端级别默认值） |
| `maxConnections(int)` | `int` | `100` | 连接池总连接数（同时也是单 Route 上限） |
| `addInterceptor(HttpInterceptor)` | `HttpInterceptor` | —（无） | 追加一个拦截器 |
| `interceptors(List<HttpInterceptor>)` | `List` | —（无） | 全量替换拦截器列表 |

> 请求级别可通过 `HttpRequest.Builder.timeout(Duration)` 覆盖响应超时，优先级高于客户端默认值。

---

## 使用示例

### 最简用法（默认配置）

```java
try (HttpClient client = HttpClientFactory.create()) {

    HttpResponse response = client.execute(
            HttpRequest.get("https://api.example.com/users")
                    .queryParam("page", "1")
                    .header("Authorization", "Bearer token123")
                    .build())
            .toCompletableFuture()
            .get();

    System.out.println(response.getStatusCode());   // 200
    System.out.println(response.getBodyAsString());  // UTF-8 响应体
}
```

### POST JSON 并设置请求级超时

```java
try (HttpClient client = HttpClientFactory.create()) {

    HttpResponse response = client.execute(
            HttpRequest.post("https://api.example.com/orders")
                    .header("Content-Type", "application/json")
                    .body("{\"product\":\"A\",\"qty\":2}", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(5))
                    .build())
            .toCompletableFuture()
            .get();

    if (response.getStatusCode() == 201) {
        System.out.println("创建成功: " + response.getBodyAsString());
    }
}
```

### 自定义连接池 + 拦截器（日志 + 指标）

```java
HttpClient client = HttpClientFactory.create(
        HttpClientOptions.builder()
                .connectTimeout(Duration.ofSeconds(3))
                .responseTimeout(Duration.ofSeconds(15))
                .maxConnections(50)
                .addInterceptor(new HttpInterceptor() {
                    @Override
                    public CompletionStage<Void> beforeRequest(HttpRequestContext context) {
                        System.out.printf("[HTTP] → %s %s%n",
                                context.getRequest().getMethod(),
                                context.getRequest().getUri());
                        return HttpInterceptor.super.beforeRequest(context);
                    }

                    @Override
                    public CompletionStage<Void> afterCompletion(HttpCompletionContext context) {
                        context.getResponse().ifPresent(r ->
                                System.out.printf("[HTTP] ← %d%n", r.getStatusCode()));
                        context.getError().ifPresent(e ->
                                System.out.printf("[HTTP] ✗ %s%n", e.getMessage()));
                        return HttpInterceptor.super.afterCompletion(context);
                    }
                })
                .build());
```

### 异步链式调用

```java
client.execute(HttpRequest.get("https://api.example.com/items").build())
        .thenApply(response -> response.getBodyAsString())
        .thenAccept(body -> System.out.println("异步收到: " + body))
        .exceptionally(error -> {
            // error 在 CompletionException 里，getCause() 为 HttpClientException
            System.err.println("请求失败: " + error.getCause().getMessage());
            return null;
        });
```

---

## 注意事项

1. **`HttpClient` 是 `AutoCloseable`**，建议用 try-with-resources 管理生命周期；`close()` 幂等，重复调用安全。
2. **非 2xx 不抛异常**：`execute` 返回的 `CompletionStage` 正常完成，需要调用方自行检查 `response.getStatusCode()`。仅网络层错误（连接拒绝、超时、协议异常）才以 `HttpClientException` 完成失败。
3. **`HttpClientException` 在 `CompletionStage` 失败路径下被 JDK 包裹为 `CompletionException`**，通过 `.getCause()` 取出原始异常。
4. **拦截器按注册顺序串行执行**，前一个 interceptor 的 `CompletionStage` 完成后才调用下一个。`onError` 和 `afterCompletion` 内部失败只打印 WARN 日志，不会影响主请求结果的传播。
5. **请求体字节数组在 `HttpRequest` 内部已做防御性复制**，传入后修改原数组不影响已构建的请求。
6. **连接池 `maxConnections` 同时作为 `setMaxConnTotal` 和 `setMaxConnPerRoute` 的值**，若需要更精细的 per-route 控制，目前需自行扩展。

---

## License

跟随项目主 LICENSE。
