# dto

## 模块定位

`infrastructure-component-dto` 是最底层的数据传输对象基类模块，提供 Command、Query、DTO、Response 和分页请求/响应模型。它不依赖其他组件，适合被所有业务和基础设施模块复用。

## 核心能力

- `Command`：命令对象基类。
- `Query`：查询对象基类。
- `DTO`：普通数据传输对象基类。
- `PageQuery`：分页查询基类，包含 `pageSize`、`pageIndex`、`orderBy`、`getOffset()` 等分页语义。
- `EntityResponse`、`SingleEntityResponse`、`MultiEntityResponse`、`PageEntityResponse`：统一响应包装。
- `ClientObject`、`Scope`：基础对象和作用域定义。

## 依赖边界

- 不依赖 Quarkus、Web、数据库、消息队列或日志组件。
- 不应引入 JSON/Jackson/JAX-RS 注解，避免 DTO 层绑定序列化框架。
- 上层模块可以依赖 dto，dto 不反向依赖任何上层模块。

## 对外 API

- `SingleEntityResponse.of(data)`：单实体响应。
- `MultiEntityResponse.of(list)`：列表响应。
- `PageEntityResponse.of(...)`：分页响应。
- `PageQuery#getOffset()`：分页偏移计算。

## 典型使用场景

- REST API 入参和返回值的统一基础类型。
- service/application 层命令和查询对象。
- 分页列表接口的入参和响应封装。

## 维护事项

- 保持 API 简单稳定，避免在基类中加入业务字段。
- 分页字段语义必须清晰，修改默认值或 offset 规则时需要评估所有调用方。
- 如需校验注解，应优先放在具体 DTO 上，不放入基础基类。

## 测试验收

- `./gradlew :infrastructure-component-dto:test` 通过。
- dto 模块源码中没有 Quarkus、JAX-RS、Jackson、数据库或 MQ import。
- 响应包装和分页计算有单元测试覆盖。

## 使用与依赖补充

**为了解决什么**：统一应用层入参、查询、响应和分页模型，避免每个服务重复定义 Command、Query、Response、PageQuery。

**如何使用**：业务命令继承 `Command`，查询对象继承 `Query` 或 `PageQuery`，接口返回值使用 `SingleEntityResponse.of(data)`、`MultiEntityResponse.of(list)`、`PageEntityResponse.of(...)`。

**当前依赖了什么**：无生产依赖，是底层基础模块。

**需要注意什么**：不要把 Web、JSON、数据库字段或业务字段塞进基类。审查时重点确认分页默认值、`orderBy` 语义和响应包装是否符合现有 API 约定。
