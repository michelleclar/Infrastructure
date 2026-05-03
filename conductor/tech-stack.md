# Tech Stack

## Language

| Language | Version |
|----------|---------|
| Java | 21 (LTS) |
| Kotlin | (Gradle build scripts only) |

Java toolchain is pinned via `java.toolchain.languageVersion = JavaLanguageVersion.of(21)` in the root `build.gradle.kts`.

## Build Tool

| Tool | Version |
|------|---------|
| Gradle | 8.x (Kotlin DSL) |
| Gradle parallel builds | Enabled (`org.gradle.parallel=true`) |
| Gradle build cache | Enabled (`org.gradle.caching=true`) |

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

## Messaging

| Component | Details |
|-----------|---------|
| Broker | Apache Pulsar |
| Quarkus integration | `quarkus-pulsar` (`infrastructure-component-quarkus/mq`) |
| Standalone API | `infrastructure-component-mq-api` (broker-agnostic interface) |
| Standalone impl | `infrastructure-component-mq-pulsar` |

## Caching

| Component | Details |
|-----------|---------|
| Distributed cache | Redis via Vert.x Redis client (`infrastructure-component-redis`) |
| Quarkus cache | `quarkus-cache`, `quarkus-redis` (`infrastructure-component-quarkus/cache`) |

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
| Proto codegen | `vertx-grpc-protoc-plugin2:4.5.13` |
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

## Infrastructure / Deployment

- Self-hosted via Docker
- Kubernetes (internal cluster)
- Native image builds supported via Quarkus Mandrel builder (`ubi9-quarkus-mandrel-builder-image:jdk-21`)

## Group & Version

```
group = org.carl
version = 1.0-BATE
```
