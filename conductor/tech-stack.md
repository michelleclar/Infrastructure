# Tech Stack

## Language

| Language | Version |
|----------|---------|
| Java | 21 (LTS) |
| Kotlin | (Gradle build scripts only) |

Java toolchain is pinned via `java.toolchain.languageVersion = JavaLanguageVersion.of(21)` in the root `build.gradle.kts`.

## Build Tool

| Tool | Details |
|------|---------|
| Gradle (Kotlin DSL) | 8.x, `kotlin.code.style=official` |
| Parallel builds | Enabled (`org.gradle.parallel=true`) |
| Build cache | Enabled (`org.gradle.caching=true`) |

## Framework

| Framework | Version |
|-----------|---------|
| Quarkus | 3.19.3 |

Quarkus BOM is used as the enforced platform for all dependency versions within Quarkus-integrated modules.

## Persistence

| Component | Details |
|-----------|---------|
| Database | PostgreSQL |
| Quarkus integration | `quarkus-jdbc-postgresql`, `quarkus-agroal` |
| Standalone ORM | JOOQ (`infrastructure-component-persistence-jooq`) |

## Caching

| Component | Details |
|-----------|---------|
| Distributed cache | Redis via Vert.x Redis client (`infrastructure-component-redis`) |
| Quarkus cache | `quarkus-cache`, `quarkus-redis` (`infrastructure-component-quarkus/cache`) |

## Messaging

| Component | Details |
|-----------|---------|
| Broker | Apache Pulsar |
| Quarkus integration | `quarkus-pulsar` (`infrastructure-component-quarkus/mq`) |
| Standalone API | `infrastructure-component-mq-api` (broker-agnostic interface) |
| Standalone impl | `infrastructure-component-mq-pulsar` |

## Search

| Component | Details |
|-----------|---------|
| Engine | Elasticsearch |
| Integration | `quarkus-elasticsearch` (`infrastructure-component-quarkus/search`) |

## Auth & Authorization

| Component | Details |
|-----------|---------|
| Protocol | OIDC |
| Provider | Keycloak |
| Integration | `quarkus-oidc`, `quarkus-keycloak` (`infrastructure-component-quarkus/authorization`) |
| Policy | `infrastructure-component-pdp` (standalone Policy Decision Point) |

## Workflow & State

| Component | Details |
|-----------|---------|
| Workflow engine | Temporal (`quarkus-temporal`, `infrastructure-component-quarkus/workflow`) |
| State machine | `infrastructure-component-statemachine` (standalone) |
| Rule engine | `infrastructure-component-rule-engine` (standalone) |

## Service Discovery

| Component | Details |
|-----------|---------|
| Registry | Consul |
| Load balancing | Stork (`consul-stork`) |
| Integration | `infrastructure-component-quarkus/discover` |

## RPC

| Component | Details |
|-----------|---------|
| Framework | Vert.x gRPC |
| gRPC modules | `infrastructure-component-qdrant-grpc`, `infrastructure-component-embedding-grpc` |

## Observability

| Component | Details |
|-----------|---------|
| Tracing/Metrics | OpenTelemetry (`quarkus-opentelemetry`) |
| Integration | `infrastructure-component-quarkus/metrics` |

## Shared Utilities

| Module | Purpose |
|--------|---------|
| `infrastructure-component-dto` | Base DTOs: Command, Query, Response, PageQuery |
| `infrastructure-component-log` | Unified logging (auto-adapts SLF4J / JBoss Logging) |
| `infrastructure-component-utils` | General utilities (String, Collection, DAG); depends on Guava, commons-collections4 |

## Deployment

- 作为 Maven 库发布到 Aliyun Maven，由各微服务引入使用
- 推荐 Docker 容器化部署

## Group & Version

```
group = org.carl
version = 1.0-BATE
```
