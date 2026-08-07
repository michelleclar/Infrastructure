# discover

## 当前定位

原 `infra-component-quarkus:discover` 已删除，能力拆为两个独立模块：

- `infra-component-discover-api`：只包含 JDK 类型和公共契约。
- `infra-component-discover-consul`：基于 Vert.x Consul Client 的实现。

两者均不依赖 Quarkus、CDI、MicroProfile Config 或 Stork。Quarkus 应用与普通 Java 应用使用相同的创建、启动和关闭 API。

## 已实现能力

- 显式服务注册和注销。
- HTTP、TCP、TTL 健康检查及 `deregisterAfter`。
- 仅发现 passing 实例。
- blocking query 监听实例列表变化。
- Consul KV 完整 properties 文档热加载。
- 完整校验后原子替换不可变配置快照。
- 断线重试、索引回退保护和上一有效配置保留。

## 依赖边界

- `infra-component-discover-api` 不得出现 Vert.x、Consul 或框架类型。
- `infra-component-discover-consul` 可以依赖 Vert.x Core 和 Vert.x Consul Client。
- 两个模块的运行时依赖守卫禁止 Quarkus、Quarkiverse Config、Stork 和 MicroProfile Config。
- 日志使用 `ILogger`。

## Kubernetes 状态

`discover-k8s` 当前只有 README，不加入 Gradle 构建、BOM 或发布流程。后续基于 EndpointSlice 和 ConfigMap watch 实现；Kubernetes 控制面负责 Pod 注册，因此不会提供应用侧注册实现。

## 验收命令

```bash
./gradlew :infra-component-discover-api:check :infra-component-discover-consul:check
```

Consul 集成测试使用 Testcontainers；Docker 不可用时由 JUnit/Testcontainers 明确标记为跳过。
