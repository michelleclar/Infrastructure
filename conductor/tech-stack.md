# Tech Stack

## Languages

| Language | Version | Usage |
|----------|---------|-------|
| Java | 21 | 主要开发语言（所有模块，toolchain 强制 Java 21） |
| Kotlin | 1.9+ | Gradle 构建脚本（build.gradle.kts） |

## Frameworks

| Framework | Version | Usage |
|-----------|---------|-------|
| Quarkus | 3.19.3 | Quarkus 集成模块（quarkus-component-*） |
| Vert.x | (via Quarkus) | 异步运行时、Redis 客户端、消息广播 |

## Databases & Storage

| Technology | Usage |
|------------|-------|
| PostgreSQL | 主数据库，via quarkus-jdbc-postgresql |
| JOOQ | 类型安全 SQL，persistence-jooq 模块 |
| Redis | 缓存层（分布式锁、泛型序列化），redis 模块 |

## Messaging

| Technology | Usage |
|------------|-------|
| Apache Pulsar | 消息队列，mq-pulsar 模块实现 mq-api 接口 |

## Other Integrations

| Technology | Usage |
|------------|-------|
| Elasticsearch | 全文搜索，quarkus-elasticsearch |
| gRPC | 嵌入向量服务、Qdrant 向量数据库通信 |
| Consul | 服务发现与负载均衡 |
| Temporal | 工作流编排 |
| OpenTelemetry | 可观测性与指标采集 |
| Keycloak / OIDC | 身份认证与权限控制 |

## Build & Deployment

| Tool | Usage |
|------|-------|
| Gradle (Kotlin DSL) | 构建系统，`kotlin.code.style=official` |
| Aliyun Maven | 制品仓库，库发布目标 |
| Docker | 推荐服务部署方式 |

## Module Structure

```
infrastructure/
├── infrastructure-component-quarkus/   # Quarkus 集成模块（需 Quarkus 依赖）
│   ├── approval, mq, web, user, cache
│   ├── search, metrics, workflow
│   ├── discover, broadcast
│   ├── persistence, authorization
└── infrastructure-component-*/        # 独立库模块（无 Quarkus 依赖）
    ├── dto, log, utils
    ├── persistence-jooq, redis
    ├── mq-api, mq-pulsar
    ├── rule-engine, pdp, statemachine
    └── embedding-grpc, qdrant-grpc
```
