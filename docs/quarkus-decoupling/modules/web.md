# web

## 当前定位

`infrastructure-component-quarkus:web` 主要包含 JAX-RS/Vert.x Web 适配、Jackson ObjectMapper producer、异常映射、过滤器、运行时上下文和部分 HTTP client builder。多数内容天然属于 Quarkus web adapter，但通用异常模型和请求 DTO 可以考虑下沉。

## 是否需要独立 core

暂不整体拆分。只建议将与 JAX-RS、Vert.x、Quarkus Jackson 无关的通用模型下沉到独立模块。

## 建议独立模块名

可选：

- `infrastructure-component-web-api`
- 或将通用异常/请求模型放入已有 `infrastructure-component-dto`

## Quarkus adapter 应保留内容

- `RouteFilter`、Vert.x `RoutingContext`、session handler。
- JAX-RS `ExceptionMapper`。
- Quarkus Jackson `ObjectMapperCustomizer` 和 producer。
- CDI runtime context helper。
- 与 Quarkus Web、REST、Vert.x route 绑定的注解和拦截器。

## 需要迁出的核心能力

- 与 HTTP 框架无关的 `ApiRequest`。
- 通用异常基类、错误码和错误原因，如果它们不依赖 `Response.Status`。
- 与 Quarkus route 无关的 HTTP client builder，如未来希望非 Quarkus 复用。
- 运行时用户抽象中不依赖 `RoutingContext` 的部分。

## 依赖边界

- adapter 可以依赖 Quarkus REST、Vert.x Web、Jackson、CDI、JAX-RS。
- core/api 层不得依赖 `jakarta.ws.rs.core.Response`、`RoutingContext`、`RouteFilter`、Quarkus Jackson。
- DTO 层应保持简单数据结构，不读取 CDI 或 Quarkus runtime。

## 拆分任务清单

- 标记当前 web 为 Quarkus adapter。
- 审查 `BaseException`、`BizException`、`SysException` 是否必须依赖 JAX-RS `Response`。
- 将不依赖 Web runtime 的请求模型和异常模型迁入 `dto` 或 `web-api`。
- 保留 exception mapper、filter、ObjectMapper producer 在 Quarkus adapter。
- 为异常到 HTTP 响应的映射补充 adapter 测试。

## 验收标准

- `./gradlew :infrastructure-component-quarkus:web:test` 通过。
- 若新增 web api 或迁入 dto，目标模块源码中没有 Quarkus、Vert.x、JAX-RS、CDI import。
- Quarkus web 模块只负责 HTTP runtime 集成，不承载跨框架业务模型。

## 模块审查补充

**解决的问题**：提供 Quarkus REST/Web 层的通用能力，包括 Jackson 配置、异常映射、请求上下文、路由过滤器、会话配置和 controller 拦截器。

**如何使用**：依赖 `infrastructure-component-quarkus:web`，使用 `web.*` 配置，例如 `web.super-user-id`、`web.session-timeout-hour`、`web.use-session`。业务 REST 层可复用全局异常模型、Jackson producer、运行时用户/请求上下文和相关注解。

**当前依赖**：`api(libs.bundles.web)`、`implementation(project(":infrastructure-component-utils"))`；测试侧依赖 authorization adapter。

**需要注意**：该模块天然是 Quarkus adapter，但 `ApiRequest`、通用异常基类、错误码等不一定必须留在 adapter。审查时重点看哪些类只依赖普通 Java/DTO，哪些类依赖 `RoutingContext`、`RouteFilter`、JAX-RS `Response` 或 CDI。
