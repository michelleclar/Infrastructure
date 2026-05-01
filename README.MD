## Infrastructure Components

基础设施组件库，为微服务提供可复用的通用能力。分为两类：

- **Quarkus 集成模块** — 依赖 Quarkus 框架，作为 Quarkus 扩展使用
- **独立库模块** — 不依赖 Quarkus，可在任意 Java 项目中使用

---

### Quarkus 集成模块 (`infrastructure-component-quarkus/`)

| 组件            | 功能                      | 核心依赖                                    |
|---------------|-------------------------|-----------------------------------------|
| authorization | OIDC/Keycloak 身份认证与权限控制 | quarkus-oidc, quarkus-keycloak          |
| web           | RESTful Web 服务层         | quarkus-rest, quarkus-jackson           |
| persistence   | 数据持久化（Quarkus 集成）       | quarkus-jdbc-postgresql, quarkus-agroal |
| mq            | 消息队列（Quarkus 集成）        | quarkus-pulsar                          |
| cache         | 本地与分布式缓存                | quarkus-cache, quarkus-redis            |
| discover      | 服务发现与负载均衡               | consul, consul-stork                    |
| workflow      | 工作流编排与执行                | quarkus-temporal                        |
| metrics       | 应用监控与指标采集               | quarkus-opentelemetry                   |
| broadcast     | 消息广播与事件总线               | quarkus-vertx                           |
| search        | 全文搜索                    | quarkus-elasticsearch                   |
| approval      | 审批流程                    | —                                       |
| user          | 用户管理                    | —                                       |

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
| statemachine     | 状态机                                        | 无                           |
| rule-engine      | 规则引擎                                       | 无                           |
| pdp              | 策略决策点                                      | 无                           |
| qdrant-grpc      | Qdrant 向量数据库 gRPC 客户端                      | gRPC                        |
| embedding-grpc   | Embedding 服务 gRPC 客户端                      | gRPC                        |
| audit            | 审计日志                                       | —                           |

---

### 完成度

**Quarkus 模块**

- [x] authorization
- [x] web
- [x] persistence
- [x] mq
- [x] cache
- [x] discover
- [x] workflow
- [x] metrics
- [ ] broadcast
- [ ] search
- [ ] approval
- [ ] user

**独立库**

- [x] dto
- [x] log
- [x] utils
- [x] persistence-jooq
- [x] redis
- [x] mq-api
- [x] mq-pulsar
- [ ] statemachine
- [ ] rule-engine
- [ ] pdp
- [ ] qdrant-grpc
- [ ] embedding-grpc
- [ ] audit

---

### 构建

```bash
./gradlew build                                         # 构建全部
./gradlew test                                          # 测试全部
./gradlew :infrastructure-component-redis:test          # 测试单模块
```

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
