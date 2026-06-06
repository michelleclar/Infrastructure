# log

## 模块定位

`infrastructure-component-log` 是统一日志抽象模块，屏蔽 SLF4J 与 JBoss Logging 的差异。仓库规范要求业务和组件统一使用 `ILogger`，禁止直接依赖 SLF4J/JBoss Logger。

## 核心能力

- `ILogger`：统一日志接口。
- `LoggerFactory`：获取 logger 的工厂。
- `LogLevel`、`LoggingConfig`、`LoggingFramework`：日志级别和后端选择。
- `Slf4jLoggerAdapter`、`JBossLoggerAdapter`：具体日志框架适配。
- `ILogAbility`：能力 mixin。

## 依赖边界

- 可以依赖 SLF4J API 和 JBoss Logging API。
- 不依赖 Quarkus，但支持 Quarkus 常用的 JBoss Logging。
- 其他模块应依赖 `ILogger`，不直接暴露 SLF4J/JBoss 类型。

## 对外 API

- `LoggerFactory.getLogger(Class<?>)`。
- `ILogger` 的 debug/info/warn/error 等日志方法。
- `ILogAbility#getLogger()` 风格的能力暴露。

## 典型使用场景

- 组件内部日志输出。
- Quarkus 和非 Quarkus 项目复用同一日志接口。
- 测试中替换或断言日志行为。

## 维护事项

- 新增日志方法时需要同步两个 adapter。
- 不要在日志组件中引入业务上下文、Web 上下文或 MDC 强绑定。
- 统一日志格式应通过底层框架配置完成，不放入 `ILogger` 接口。

## 测试验收

- `./gradlew :infrastructure-component-log:test` 通过。
- SLF4J 与 JBoss adapter 都有基本调用测试。
- 仓库源码中新增日志使用应优先调用 `LoggerFactory.getLogger(...)`。

## 使用与依赖补充

**为了解决什么**：统一日志门面，让组件既能在普通 Java/SLF4J 环境运行，也能在 Quarkus/JBoss Logging 环境运行。

**如何使用**：在类中声明 `private static final ILogger LOGGER = LoggerFactory.getLogger(MyClass.class);`，通过 `LOGGER.info(...)`、`LOGGER.error(...)` 等方法输出日志。能力类可实现 `ILogAbility`。

**当前依赖了什么**：`org.slf4j:slf4j-api:2.0.9`、`org.jboss.logging:jboss-logging:3.5.3.Final`，测试使用 JUnit 和 logback。

**需要注意什么**：新增日志能力必须同步 SLF4J/JBoss 两个 adapter。审查时要把直接使用 SLF4J/JBoss Logger 的代码改回 `ILogger`。
