# utils

## 模块定位

`infrastructure-component-utils` 是通用工具组件，提供字符串、集合、字段反射、URL 解析、日志辅助、脱敏算法、基础数据结构、加密/hash、JSONB/Jackson、UTC 时间和分页归一化工具。

## 核心能力

- `StringUtils`、`CollectionUtils`。
- `FieldsUtils`：字段反射相关工具。
- `UrlParser`：URL 解析。
- `LogUtils`：日志辅助工具。
- desensitization：邮箱、手机号、中间字符等脱敏算法。
- struct：`DAG`、`LinkedTable`。
- crypto：`AesGcmStringCipher`、`VersionedCiphertext`。
- hash：`Sha256`。
- json：`SharedObjectMapper`、`JsonbConverter`。
- time：`UtcClock`。
- pagination：`Pagination`、`PageWindow`。

## 依赖边界

- 仅允许基础工具所需的稳定依赖，例如 Jackson 和 jOOQ `JSONB` 类型。
- 不依赖 Quarkus、CDI、Web runtime、数据库连接或 MQ；JSONB 仅使用 jOOQ 值类型。
- 工具方法不能反向依赖业务组件。

## 对外 API

- 字符串和集合判空、转换、格式化工具。
- 脱敏算法接口 `IDesensitizationAlgorithm` 及默认实现。
- `DAG` 用于有向无环图建模和依赖排序。
- `LinkedTable` 用于链式结构或表结构辅助。

## 典型使用场景

- 基础组件内部复用工具方法。
- API 输出前做数据脱敏。
- workflow、rule engine 等场景处理 DAG 依赖。

## 维护事项

- 工具类应保持小而稳定，避免成为业务逻辑堆放点。
- 新增工具前先确认是否应属于更具体的组件。
- 反射工具要注意 JDK 21 模块访问和性能影响。

## 测试验收

- `./gradlew :infrastructure-component-utils:test` 通过。
- 字符串、集合、脱敏、DAG、AES-GCM、SHA-256、JSONB、UTC 时间和分页工具有边界用例测试。
- 源码中没有 Quarkus、CDI、JAX-RS、MicroProfile Config import。

## 使用与依赖补充

**为了解决什么**：沉淀跨模块通用的小工具，避免字符串、集合、脱敏、DAG 等基础逻辑在各组件里重复实现。

**如何使用**：直接调用 `StringUtils`、`CollectionUtils`、`UrlParser`、`FieldsUtils` 等静态工具；脱敏场景使用 `IDesensitizationAlgorithm` 及默认算法；依赖排序或流程拓扑可使用 `DAG`。

**当前依赖了什么**：Jackson databind、Jackson JavaTimeModule、jOOQ `JSONB` 类型，测试依赖 JUnit。

**需要注意什么**：utils 容易膨胀成杂物间。审查新增工具时要判断它是否更适合放到具体组件，例如 persistence、web、redis 或 rule-engine。
