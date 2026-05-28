# Infrastructure Components — Completion PRD

## Background

This repository is a multi-module Gradle infrastructure component library (`org.carl.infrastructure.*`) for Quarkus-based microservices. It is a **library, not a runnable application**. Components are divided into Quarkus integration modules and standalone library modules with no Quarkus dependency.

The core Quarkus integrations (authorization, web, persistence, mq, cache, discover, workflow, metrics) and standalone libs (dto, log, utils, persistence-jooq, redis, mq-api, mq-pulsar) are complete. This PRD covers the **10 remaining incomplete modules**.

## Goal

Complete all incomplete modules so the library provides full coverage of the intended infrastructure capabilities: event bus, full-text search, policy/governance, AI/vector-DB access, and workflow-adjacent business services.

## Scope

**In scope:** Implement, test, and document the 10 modules listed in the milestone table.  
**Out of scope:** Modifications to existing complete modules, native build changes, new modules not listed in the README checklist.

## Constraints

- Java 21, Quarkus 3.x for Quarkus modules; no Quarkus dependency for standalone libs
- All modules must follow the Ability-interface pattern documented in `AGENTS.md`
- Integration tests must use `assumeTrue()` guards for external service dependencies
- `src/main/gen/` is codegen-only; no hand-edits
- Each module must have a corresponding entry in `demo/` or inline usage example

---

## Milestones

| Phase | Name | Scope | Target |
|-------|------|-------|--------|
| 1 | Core Independent Libraries | Implement `statemachine`, `rule-engine`, and `pdp` standalone modules. Each must expose an Ability interface, have unit tests, and be documented in `doc/AI_MODULE_GUIDE.md`. | All three modules build, pass tests, and are demonstrable without Quarkus. |
| 2 | AI and gRPC Clients | Implement `qdrant-grpc` (Qdrant vector DB client) and `embedding-grpc` (Embedding service client) standalone modules. Includes proto definitions, generated stubs, and typed wrappers. | Both clients connect to their respective services in integration tests; stubs are auto-generated from proto files. |
| 3 | Quarkus Integration Modules | Implement `broadcast` (Vert.x event bus) and `search` (Elasticsearch full-text search) as Quarkus extensions under `infrastructure-component-quarkus/`. Both follow the conditional bean registration pattern (`quarkus.plugins.<name>.enable`). | Both extensions register correctly in a Quarkus app; demo wiring in `demo/` passes `./gradlew :demo:build`. |
| 4 | Business Logic Modules | Implement `approval` (approval workflow), `user` (user management), and `audit` (audit log) as Quarkus integration modules. These may depend on Phase 1 modules (statemachine, rule-engine). | All three modules pass tests; `audit` captures structured events; `approval` integrates with `statemachine`. |
