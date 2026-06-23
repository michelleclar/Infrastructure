# Infrastructure Reusable Component Extraction (Wave 2)

## Background

Several app-neutral capabilities are currently duplicated inside the **ER Tool** application. This PRD extracts them into reusable `org.carl.infrastructure.*` modules so applications depend on infrastructure instead of re-implementing them.

## Goal

Deliver reusable, app-neutral infrastructure components with tests and documentation, removing ER Tool duplication.

## Scope

**In scope:** INFRA-1 (web response envelope + exception mapping), INFRA-4 (artifact storage), INFRA-5 (shared crypto/hash/json/time utilities).

**Out of scope — deferred, separate re-planning (database-tooling cluster):** INFRA-2 (external JDBC connection & execution), INFRA-3 (database metadata reader / introspection), INFRA-6 (neutral DDL/schema model utilities).

**Untriaged (not yet planned):** INFRA-36 (jOOQ soft-delete/optimistic-lock templates), INFRA-37 (Pulsar Source Admin + Debezium envelope).

## Constraints

- Java 21; standalone libs carry no Quarkus dependency unless the capability is inherently Quarkus-specific.
- App-neutral: no ER Tool imports, no business error codes.
- Tests required; integration tests use `assumeTrue()` guards for external dependencies.
- JSONB/Jackson conversion reuses the shared `ObjectMapper` configuration.

## Milestones

| Phase | Name | Scope | Target |
|-------|------|-------|--------|
| 1 | Reusable Standalone Components | Implement INFRA-1 (web response envelope & exception mapping: request id, validation errors, business exceptions, fallback errors, configurable envelope), INFRA-4 (artifact storage: local provider, path-traversal protection, metadata, provider abstraction for future MinIO/OSS), and INFRA-5 (shared crypto/hash/json/time utilities: AES-GCM versioned cipher, SHA-256, JSONB/Jackson, UTC clock, pagination normalization). The three children run in parallel. | All three modules build, pass tests, expose app-neutral APIs with no ER Tool imports, and can replace the corresponding ER Tool duplication. |
