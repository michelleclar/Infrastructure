## Infrastructure Components

基础设施组件库，为微服务提供可复用的通用能力。分为两类：

- **Quarkus 集成模块** — 依赖 Quarkus 框架，作为 Quarkus 扩展使用
- **独立库模块** — 不依赖 Quarkus，可在任意 Java 项目中使用

---

### Quarkus 集成模块 (`infra-component-quarkus/`)

| 组件            | 功能                      | 核心依赖                                    |
|---------------|-------------------------|-----------------------------------------|
| authorization | OIDC/Keycloak 身份认证与权限控制 | quarkus-oidc, quarkus-keycloak          |
| web           | RESTful Web 服务层         | quarkus-rest, quarkus-jackson           |
| persistence   | 数据持久化（Quarkus 集成）       | quarkus-jdbc-postgresql, quarkus-agroal |
| mq            | 消息队列（Quarkus 集成）        | quarkus-pulsar                          |
| cache         | 本地与分布式缓存                | quarkus-cache, quarkus-redis            |
| workflow      | 工作流编排与执行                | quarkus-temporal                        |
| metrics       | 应用监控与指标采集               | quarkus-opentelemetry                   |
| search        | 全文搜索                    | quarkus-elasticsearch                   |

### 独立库模块（无 Quarkus 依赖）

| 组件               | 功能                                         | 核心依赖                        |
|------------------|--------------------------------------------|-----------------------------|
| dto              | 数据传输对象基类（Command/Query/Response/PageQuery） | 无                           |
| log              | 统一日志接口（自动适配 SLF4J/JBoss）                   | SLF4J, JBoss Logging        |
| utils            | 通用工具类（字符串、集合、DAG 等）                        | Guava, commons-collections4 |
| persistence-jooq | JOOQ 数据库操作封装                               | JOOQ, PostgreSQL            |
| redis            | Redis 客户端（异步/Sync、分布式锁、泛型序列化）              | Vert.x Redis                |
| mq-api           | 消息队列抽象接口                                   | 无                           |
| mq-pulsar        | Pulsar 消息队列实现                              | Apache Pulsar               |
| infra-component-discover-api | 注册发现与动态配置公共 API                  | JDK                         |
| infra-component-discover-consul | Consul 注册、健康发现与 KV 配置热加载       | Vert.x Consul               |
| discover-k8s     | Kubernetes 发现与配置热加载（仅占位，尚未实现）            | —                           |
| statemachine     | 状态机                                        | 无                           |
| rule-engine      | 规则引擎                                       | 无                           |
| pdp              | 策略决策点                                      | 无                           |
| qdrant-grpc      | Qdrant 向量数据库 gRPC 客户端                      | gRPC                        |
| embedding-grpc   | Embedding 服务 gRPC 客户端                      | gRPC                        |

---

### 完成度

**Quarkus 模块**

- [x] authorization
- [x] web
- [x] persistence
- [x] mq
- [x] cache
- [x] workflow
- [x] metrics
- [ ] search

**独立库**

- [x] dto
- [x] log
- [x] utils
- [x] persistence-jooq
- [x] redis
- [x] mq-api
- [x] mq-pulsar
- [x] infra-component-discover-api
- [x] infra-component-discover-consul
- [ ] discover-k8s
- [ ] statemachine
- [ ] rule-engine
- [ ] pdp
- [ ] qdrant-grpc
- [ ] embedding-grpc

---

### 构建

```bash
./gradlew build                                         # 构建全部
./gradlew test                                          # 测试全部
./gradlew :infra-component-redis:test          # 测试单模块
./gradlew :infra-component-discover-consul:test # 测试 Consul 注册发现与动态配置
```

### Consul 注册发现与动态配置

`infra-component-discover-consul` 是普通 Java 21 库，不依赖 Quarkus、CDI、MicroProfile Config 或 Stork。

```java
ConsulDiscoverOptions options =
    ConsulDiscoverOptions.builder(
            URI.create("http://127.0.0.1:8500"),
            "config/orders")
        .addValidator(snapshot -> snapshot.get("order.timeout", Duration.class))
        .build();

ConsulDiscoverClient client = ConsulDiscoverClient.create(options);
client.start().toCompletableFuture().join();

client.register(new ServiceRegistration(
    "orders",
    "orders-10.0.0.8-8080",
    "10.0.0.8",
    8080
)).toCompletableFuture().join();

List<ServiceInstance> instances =
    client.discover(new ServiceQuery("orders")).toCompletableFuture().join();

Duration timeout = client.current().get("order.timeout", Duration.class);
```

配置 KV 的值必须是完整的 properties 文档。新版本只有在完整解析并通过全部校验器后才会原子生效；错误版本不会覆盖上一有效快照。

### Native 构建

```bash
# 不使用容器
./gradlew build \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.enabled=true

docker build -f src/main/docker/Dockerfile.native-micro -t embedding/embedding-native-micro:1.0 .
```

```bash
# 使用容器构建（跨平台）
./gradlew build \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true \
  -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21.0.5
```

```bash
# 构建 x86 amd linux（仅限容器方式）
./gradlew build \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true \
  -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21.0.5 \
  -Dquarkus.native.container-runtime-options=--platform=linux/amd64
```
