# discover-k8s

`discover-k8s` 当前未实现，也未加入 `settings.gradle.kts`、`infra-bom` 或发布流程。

后续实现范围：

- 基于 Kubernetes `discovery.k8s.io/v1` EndpointSlice 的服务发现。
- 监听 ConfigMap 并按完整 `application.properties` 文档原子更新动态配置。
- 复用 `infra-component-discover-api` 中的公共接口和不可变数据模型。

Kubernetes 中的 Pod 注册、摘除由 Deployment、Service 和控制面负责，因此本模块不会实现应用侧服务注册。
