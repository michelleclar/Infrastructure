# Spec

Implement `infrastructure-component-http` as an independent Java 21 library module.

The component provides a small stable API over Apache HttpComponents Client 5 Async. It is intended for non-Quarkus and Quarkus consumers, so the public API must avoid Quarkus, Vert.x, and Apache implementation types.

The component must support asynchronous request execution, basic request/response modeling, configurable client options, and a full lifecycle interceptor chain.

## Requirements

- Add a new Gradle module: `infrastructure-component-http`.
- Use Apache HttpComponents Client 5 Async as the underlying client.
- Keep public APIs under `org.carl.infrastructure.http`.
- Do not expose Apache client types through the public component API.
- Support async request execution through `CompletionStage`.
- Support GET, POST, PUT, DELETE, and custom HTTP methods.
- Support headers, query params, timeout, string body, and byte array body.
- Return response status, reason phrase, headers, and body bytes/string.
- Return 4xx/5xx responses normally; throw component exceptions for transport/client failures.
- Implement lifecycle interceptors:
  - `beforeRequest`
  - `beforeSend`
  - `afterResponseHeaders`
  - `afterResponseBody`
  - `onError`
  - `afterCompletion`
- Interceptor hooks must support asynchronous execution.
- Use `ILogger` for internal logging, not SLF4J/JBoss directly.
- Add local tests without external network dependency.

## Acceptance Criteria

- `./gradlew :infrastructure-component-http:test` passes.
- New module is included in `settings.gradle.kts`.
- Dependency catalog contains the selected Apache HttpComponents dependency.
- Interceptor order is tested for success, error, and timeout flows.
- HTTP client can be closed safely through `AutoCloseable`.
