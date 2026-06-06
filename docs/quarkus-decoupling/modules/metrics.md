# metrics

## 当前定位

`infrastructure-component-quarkus:metrics` 目前几乎没有业务源码，适合作为 Quarkus OpenTelemetry、Micrometer 或 SmallRye Metrics 的 adapter 占位模块。

## 是否需要独立 core

暂不需要。只有在出现跨运行时指标抽象、统一埋点 API 或指标命名规范需要编译期复用时，才创建独立 core。

## 建议独立模块名

可选：`infrastructure-component-metrics-api`

## Quarkus adapter 应保留内容

- Quarkus OpenTelemetry 或 Micrometer 扩展依赖。
- 指标 exporter、resource attributes、采样率等配置映射。
- CDI producer 或拦截器注册。
- 与 Quarkus HTTP、REST、Scheduler、Messaging 相关的指标绑定。

## 需要迁出的核心能力

- 可选迁出指标名称、标签常量和通用埋点接口。
- 可选迁出与具体运行时无关的计数器、计时器、追踪上下文抽象。

## 依赖边界

- adapter 可以依赖 Quarkus metrics/OpenTelemetry 扩展。
- core/api 层不得依赖 Quarkus、CDI、MicroProfile Config。
- 如果选择直接依赖 OpenTelemetry API，应区分 OpenTelemetry 标准 API 与 Quarkus extension。

## 拆分任务清单

- 保持当前 metrics 为 adapter 占位模块。
- 明确新增代码必须是 Quarkus 指标集成或配置装配。
- 如业务模块需要统一埋点 API，再创建 metrics api。
- 指标命名规范优先写入文档或常量类，避免分散在各 adapter 中。

## 验收标准

- `./gradlew :infrastructure-component-quarkus:metrics:test` 通过。
- metrics adapter 不承载业务指标计算规则。
- 若新增 metrics api，其源码中没有 Quarkus、CDI、MicroProfile Config import。

## 模块审查补充

**解决的问题**：作为 Quarkus 指标和链路追踪能力的接入点，统一承载 OpenTelemetry、Micrometer 或 SmallRye Metrics 的依赖和配置。

**如何使用**：依赖 `infrastructure-component-quarkus:metrics` 后，通过 Quarkus 原生 metrics/tracing 配置启用 exporter、采样、resource attributes 等能力。当前模块源码较少，更像依赖聚合和未来扩展占位。

**当前依赖**：`implementation(libs.bundles.metrics)`。

**需要注意**：不要把业务指标计算逻辑写进该模块。该模块应只做运行时指标接线、拦截器、exporter 配置或标准 tags。若业务需要统一埋点 API，再新增 `metrics-api`。
