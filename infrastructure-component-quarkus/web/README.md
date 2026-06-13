# infrastructure-component-quarkus / web

为 Quarkus 应用提供开箱即用的 Web 层基础设施，涵盖：全局异常处理、Jackson 序列化定制、
CDI 拦截器注解（控制器日志、限流占位、防重复提交占位）、运行时请求上下文（当前用户 / 请求路径解析）、
路由过滤器（Session 注入 + 用户绑定），以及一个基于 Apache HttpClient 的同步 HTTP 客户端构建器。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:web"))
}
```

本模块以 `api` 方式传递以下 Quarkus 扩展（消费方无需重复声明）：

| Gradle 别名 | 实际坐标 |
|---|---|
| `quarkus-rest` | `io.quarkus:quarkus-rest` |
| `quarkus-jackson` | `io.quarkus:quarkus-rest-jackson` |
| `quarkus-smallrye-openapi` | `io.quarkus:quarkus-smallrye-openapi` |
| `quarkus-reactive-routes` | `io.quarkus:quarkus-reactive-routes` |
| `quarkus-vertx` | `io.quarkus:quarkus-vertx` |
| `quarkus-smallrye-fault-tolerance` | `io.quarkus:quarkus-smallrye-fault-tolerance` |
| `quarkus-httpclient` | `io.quarkus:quarkus-apache-httpclient` |

此外还依赖 `:infrastructure-component-utils`（实现依赖，不传递）。

---

## 配置项

本模块通过 `@ConfigMapping(prefix = "web")` 绑定配置，对应接口为
`org.carl.infrastructure.component.web.config.WebConfig`。

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `web.super-user-id` | `String` | `"1"` | 超级用户 ID，`WebConfig.isSuperUser(userId)` 用于鉴权判断 |
| `web.session-timeout-hour` | `int` | `24` | Session 超时时间（小时），转换为毫秒后传给 Vert.x `SessionHandler` |
| `web.use-session` | `boolean` | `true` | 是否启用 Session；`false` 时 `RootFilter` 跳过 Session 注册 |

运行环境（`quarkus.profile`）通过 `IProfile` 接口在运行期读取，枚举值为
`ProfileMode.DEV` / `PROD` / `TEST`。`WebConfig` 继承 `IProfile`，
注入后可直接调用 `config.isDevMode()` / `isProdMode()` / `isTestMode()`。

---

## 核心能力

### 1. 全局异常处理

包路径：`org.carl.infrastructure.component.web.config.exception` /
`org.carl.infrastructure.component.web.config.global`

| 类 / 接口 | 说明 |
|---|---|
| `ExceptionReason` | 异常描述接口：`getReason()` / `getScenario()` / `getErrorType()` / `getCode()` |
| `BaseException` | 所有业务 / 系统异常的基类；持有 `ExceptionReason`，`toResponse()` 直接构造 JAX-RS `Response` |
| `BizException` | 业务异常，`errorType` 默认 `"BIZ_ERROR"`；提供四个静态工厂：`biz(reason)` / `biz(reason, code)` / `biz(reason, errorType, scenario)` / `biz(reason, errorType, scenario, code)` |
| `SysException` | 系统异常，`errorType` 默认 `"SYS_ERROR"`；静态工厂：`sys(reason)` / `sys(reason, code)` |
| `DefaultGlobalExceptionHandler` | `@Provider`，实现 `ExceptionMapper<Exception>`；分派 `SysException` / `BizException`，未匹配的异常统一返回 HTTP 500，响应体为 JSON 格式的 `ExceptionReason` |

**抛出异常示例：**

```java
// 业务异常，HTTP code = -1（由调用方决定响应码）
throw BizException.biz("用户不存在");

// 业务异常，指定 HTTP code
throw BizException.biz("订单已关闭", 409);

// 系统异常，固定 HTTP 500
throw SysException.sys("数据库连接超时");
```

`DefaultGlobalExceptionHandler` 自动捕获上述异常并序列化为 JSON 响应，无需在每个 Resource 类中重复处理。

---

### 2. Jackson 序列化

包路径：`org.carl.infrastructure.component.web.config`
/ `org.carl.infrastructure.component.web.config.global`

| 类 / 枚举 | 说明 |
|---|---|
| `JacksonProvider` | 枚举单例 `JACKSON`，持有预配置的 `ObjectMapper`；注册了以下自定义序列化器（见下表） |
| `ObjectMapperConfig` | `@ApplicationScoped`，通过 `@Produces @Singleton` 将 `JacksonProvider.JACKSON.get()` 注册为 CDI `ObjectMapper` bean，同时接受所有 `ObjectMapperCustomizer` 进行二次定制 |
| `JSON` | 枚举单例 `JSON`，实现 `JacksonAbility`；可在非 CDI 上下文（如静态工具方法）中直接调用 `JSON.JSON.toJsonStringX(obj)` |

**内置自定义序列化格式：**

| Java 类型 | 序列化格式 |
|---|---|
| `java.util.Date`（非 `java.sql.Date`） | `"yyyy-MM-dd HH:mm:ss"` |
| `java.sql.Date` | `"yyyy-MM-dd"` |
| `java.time.LocalDateTime` | `"yyyy-MM-dd HH:mm:ss"` |
| `java.time.LocalDate` | `"yyyy-MM-dd"` |
| `java.time.Duration` | 秒数（`long`，通过 `writeNumber`） |
| `java.time.OffsetDateTime` | ISO-8601 带偏移格式（`DateTimeFormatter.ISO_OFFSET_DATE_TIME`） |

---

### 3. 拦截器注解

包路径：`org.carl.infrastructure.component.web.annotations` /
`org.carl.infrastructure.component.web.annotations.interceptor`

#### 3.1 `@ControllerLogged` + `ControllerLoggedInterceptor`

标注在 JAX-RS Resource 方法或类上，拦截器在 DEBUG 级别记录请求参数（序列化为 JSON）和响应内容（含耗时），
并捕获所有异常统一转换为 JAX-RS `Response`——**即标注了此注解的方法不会向外抛出异常**。

- 优先级（`@Priority`）：`1`
- 对 `Uni` / `Multi` 返回值的响应日志采用响应式回调方式记录，不阻塞事件循环
- 异常转换规则与 `DefaultGlobalExceptionHandler` 一致：`BizException` → warn 日志，`SysException` → error 日志，其余 → HTTP -999

```java
@Path("/orders")
@ControllerLogged          // 作用于整个 Resource 类
public class OrderResource {

    @GET
    @Path("/{id}")
    public Uni<Order> get(@PathParam("id") String id) {
        // 方法内抛出的 BizException / SysException 会被拦截器捕获并转为 Response
        return orderService.findById(id);
    }
}
```

#### 3.2 `@Limit` + `LimitInterceptor`

`@InterceptorBinding`，`@Target({TYPE, METHOD})`。

`LimitInterceptor` 当前**全部注释掉**（文件内容为注释代码），注解本身已注册但拦截器尚未激活。
实际使用前需确认拦截器已启用。

#### 3.3 `@PreventDuplicateValidator` + `DuplicateInterceptor`

`@InterceptorBinding`，`@Target({TYPE, METHOD})`。

`DuplicateInterceptor` 当前**全部注释掉**（文件内容为注释代码），注解本身已注册但拦截器尚未激活。
注解声明的属性如下：

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `includeFieldKeys` | `String[]` | `{}` | 参与防重复 key 构造的请求字段名 |
| `optionalValues` | `String[]` | `{}` | 追加到 key 末尾的可选固定值 |
| `expireTime` | `long` | `10000`（毫秒） | 防重复窗口时长 |

---

### 4. 运行时上下文

包路径：`org.carl.infrastructure.component.web.runtime` /
`org.carl.infrastructure.component.web.ability`

| 类 / 接口 | 说明 |
|---|---|
| `IRuntimeUser` | 当前请求用户契约接口：`getId()` / `getName()` / `getUsername()` / `getOrganizationId()` / `getDepartmentId()`；内置白名单用户常量 `IRuntimeUser.WHITE`（id=`"0"`）；提供两个静态工厂：`build(String id)` / `build(JsonObject object)` |
| `IRuntimeProvider` | 用户解析 SPI；业务模块实现此接口后由 `RootFilter` 通过 CDI `Instance<IRuntimeProvider>` 调用；`getUser(RoutingContext)` 返回 `Optional<IRuntimeUser>`，`apiRequest(RoutingContext)` 构造 `ApiRequest` |
| `ApplicationContextHelper` | 静态工具类，`getBean(Class<T>)` 通过 `CDI.current().select(clazz).get()` 获取 CDI bean；解析失败时记录 warn 日志并返回 `null` |
| `IRuntimeAbility` | 混入接口，继承 `IProfile`；要求实现类提供 `getRoutingContext()`；暴露 `getApiRequest()` 和 `getUser()` 两个默认方法，从 `RoutingContext` 中取出 `RootFilter` 注入的上下文对象 |
| `ApiRequest` | 解析 `/api/{version}/{module(.submodule)}/{action}[/{dataId}]` 格式的路径；不符合此格式的路径 `isSkip()` 返回 `true`；常量 `ApiRequest.BLANK` 表示空白请求 |

**`IRuntimeAbility` 使用示例：**

```java
@ApplicationScoped
public class OrderService implements IRuntimeAbility {

    @Inject
    RoutingContext routingContext;

    @Override
    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    public void doSomething() {
        IRuntimeUser user = getUser();       // 从 RoutingContext 取出当前用户
        ApiRequest req = getApiRequest();    // 取出已解析的请求路径对象
        boolean isProd = isProdMode();       // 来自 IProfile
    }
}
```

---

### 5. 路由过滤器

类：`org.carl.infrastructure.component.web.filter.RootFilter`（`@ApplicationScoped`）

启动时（`@Observes StartupEvent`）若 `web.use-session=true`，创建 Vert.x `LocalSessionStore` 并配置超时。

注册三条 Reactive Routes 过滤器，按优先级从低到高执行：

| 过滤器方法 | 优先级 | 说明 |
|---|---|---|
| `apiRequestFilter` | `Priorities.HEADER_DECORATOR` | **已标注 `@Deprecated`**；若 `IRuntimeProvider` 可解析则构造 `ApiRequest` 并写入 `RoutingContext`（key：`"apiRequest"`），否则写入 `ApiRequest.BLANK` |
| `sessionFilter` | `Priorities.USER + 100` | 路径以 `/api` 开头且 `web.use-session=true` 时，挂载 `SessionHandler` |
| `userFilter` | `Priorities.USER` | **已标注 `@Deprecated`**；调用 `IRuntimeProvider.getUser(context)` 解析用户；测试模式下若请求头含 `userId` 则通过 `IRuntimeUser.build(userId)` 构造用户；兜底为 `IRuntimeUser.WHITE`；结果写入 `RoutingContext`（key：`"runtimeUser"`） |

> `userFilter` 和 `apiRequestFilter` 均已标注 `@Deprecated`，表明这两条过滤器路径在后续版本可能调整。

---

### 6. Jackson 能力混入（`JacksonAbility`）

接口：`org.carl.infrastructure.component.web.ability.JacksonAbility`

任何类实现此接口后，即可通过默认方法直接调用 JSON 序列化/反序列化，底层使用 `JacksonProvider.JACKSON`。

| 方法 | 说明 |
|---|---|
| `toJsonString(Object)` | 序列化为 JSON 字符串，抛出受检 `JsonProcessingException` |
| `toJsonStringX(Object)` | 序列化为 JSON 字符串，失败时抛出 `BizException`（非受检） |
| `toJsonNode(String)` | 解析为 `JsonNode`，抛出受检异常 |
| `toJsonNodeX(String)` | 解析为 `JsonNode`，失败时抛出 `BizException` |
| `toJsonObjectX(Object)` | 转换为 `ObjectNode` |
| `fromJson(String, Class<T>)` | 反序列化为指定类型，抛出受检异常 |
| `fromJsonX(String, Class<T>)` | 反序列化为指定类型，失败时抛出 `BizException` |
| `fromJson(String, TypeReference<T>)` | 反序列化（泛型容器），抛出受检异常 |
| `convertValue(Object, Class<T>)` | 对象类型转换 |
| `convertValue(Object, TypeReference<T>)` | 对象类型转换（泛型容器） |
| `mapToJsonNode(Map<String, Object>)` | `Map` 转 `ObjectNode` |
| `createObjectNode()` | 创建空 `ObjectNode` |
| `getObjectMapper()` | 获取底层 `ObjectMapper` |
| `merge(ObjectNode, JsonNode)` | 浅合并：将 `target` 的全部字段写入 `source` 的深拷贝 |
| `mergeJsonNodes(JsonNode, JsonNode)` | 合并两个节点：同名字段组合为 `ArrayNode` |
| `toMap(Object)` | 对象转 `Map<String, Object>` |

`JSON.JSON`（`org.carl.infrastructure.component.web.config.JSON`）是实现了 `JacksonAbility` 的枚举单例，可在静态上下文中直接使用：

```java
String json = JSON.JSON.toJsonStringX(myObject);
MyClass obj = JSON.JSON.fromJsonX(json, MyClass.class);
```

---

### 7. HTTP 客户端

类：`org.carl.infrastructure.component.web.utils.HClientBuilder` /
`org.carl.infrastructure.component.web.utils.HClient`

基于 `org.apache.httpcomponents:httpclient`（通过 `quarkus-apache-httpclient` 引入）的同步 HTTP 客户端。
`HClient` 实现 `AutoCloseable`，用完后需关闭。当前仅支持 POST 方法。

```java
try (HClient client = HClientBuilder.create("https://example.com/api/notify")
        .entity(myRequestBody)                        // 序列化为 JSON 请求体
        .headers(h -> {
            h.put("Content-Type", "application/json");
            h.put("Authorization", "Bearer " + token);
        })
        .method(Route.HttpMethod.POST)                // 当前仅 POST 有效
        .build()) {

    String responseBody = client.executer();
}
```

| `HClientBuilder` 方法 | 说明 |
|---|---|
| `HClientBuilder.create(String url)` | 静态工厂，指定目标 URL |
| `entity(Object)` | 请求体；内部通过 `ObjectMapper.writeValueAsString` 序列化 |
| `headers(Map<String, String>)` | 直接设置请求头 Map |
| `headers(Consumer<Map<String, String>>)` | 通过 Lambda 修改请求头 |
| `method(Route.HttpMethod)` | 指定 HTTP 方法（目前仅 `POST` 分支有实现） |
| `build()` | 返回 `HClient` 实例 |

---

### 8. 自定义 HTTP 状态类型

类：`org.carl.infrastructure.component.web.config.StatusType`

实现 `jakarta.ws.rs.core.Response.StatusType`，用于在 JAX-RS `Response.status(...)` 中使用自定义状态码。

当前预定义常量：

| 常量 | 状态码 | Reason Phrase |
|---|---|---|
| `StatusType.ERROR_DUPLICATE` | `467` | `"ERROR_DUPLICATE"` |

---

## 注意事项

1. **`@ControllerLogged` 会吞掉所有异常**：拦截器内部 catch 了全部 `Throwable` 并转为 `Response`，标注此注解的方法永远不会向调用方抛出异常。如果上层需要感知异常，不应使用此注解。

2. **`LimitInterceptor` 和 `DuplicateInterceptor` 均未激活**：两个拦截器类的实现代码全部被注释掉，对应注解 `@Limit` 和 `@PreventDuplicateValidator` 标注后无实际效果。

3. **`RootFilter` 的用户解析依赖 `IRuntimeProvider` CDI bean**：若消费方未提供 `IRuntimeProvider` 实现，`Instance<IRuntimeProvider>.isResolvable()` 为 `false`，用户上下文统一为 `IRuntimeUser.WHITE`。

4. **`HClient` 仅实现了 POST**：`executer()` 中 `switch` 表达式对非 POST 方法返回 `null`，使用前确认请求方法。

5. **`ApplicationContextHelper.getBean` 在容器启动完成前调用会返回 `null`**：建议仅在请求上下文或 `@PostConstruct` 之后调用。

6. **`ApiRequest` 的路径解析要求严格格式**：路径必须以 `/api` 开头且至少有 5 个 `/` 分段（`/api/{version}/{module}/{action}`），否则 `isSkip()` 返回 `true`，各字段均为 `null`。

7. **`ObjectMapperConfig` 产出的 CDI `ObjectMapper` bean 作用域为 `@Singleton`**：注入的所有 `ObjectMapperCustomizer` 在启动时执行一次，运行期不可更改。
