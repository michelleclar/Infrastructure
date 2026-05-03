# Java Style Guide

Applies to all Java source files in this project (Java 21, compiled with `-parameters`).

## Formatting

- **Indentation**: 4 spaces, no tabs.
- **Line length**: 120 characters max.
- **Braces**: Allman-adjacent (opening brace on same line as declaration).
- **Blank lines**: One blank line between methods; two between top-level declarations.
- **Trailing whitespace**: Not permitted.
- **File encoding**: UTF-8 (enforced via `options.encoding = "UTF-8"` in Gradle).

## Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Classes / Interfaces | UpperCamelCase | `RedisClientFactory` |
| Methods / Variables | lowerCamelCase | `buildConnection()` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_TIMEOUT_MS` |
| Packages | all lowercase, dot-separated | `org.carl.infrastructure.redis` |
| Test classes | suffix `Test` | `RedisClientFactoryTest` |

## Imports

- No wildcard imports (`import java.util.*` is forbidden).
- Static imports allowed for test assertions (`assertThat`, `assertEquals`).
- Import order: Java standard library → Jakarta/Javax → third-party → internal (`org.carl`).

## Module Structure

Every module must follow this layout:

```
src/
  main/
    java/org/carl/infrastructure/<module>/
      api/          # Public interfaces and DTOs (stable contract)
      impl/         # Internal implementations (not part of public API)
      config/       # Configuration classes
  test/
    java/org/carl/infrastructure/<module>/
```

## API Visibility

- Public interfaces live under `api/` and are part of the public contract.
- Implementation classes under `impl/` must be package-private or annotated `@Internal` where a public modifier is unavoidable.
- Do not expose concrete implementation types in method signatures — use the interface type.

## Quarkus-Specific Rules (Quarkus modules only)

- Inject dependencies via `@Inject` (CDI), not constructor injection, unless the class is not a CDI bean.
- Configuration must use `@ConfigMapping` or `@ConfigProperty`; no hardcoded values.
- Startup validation goes in a method annotated `@Observes StartupEvent`; never in a constructor.
- All CDI beans used in tests must be annotated `@QuarkusTest` with `@InjectMock` for external dependencies.

## Testing

- Every public API method requires at least one unit test.
- Test method names follow the pattern: `should_<expected>_when_<condition>`.
- Use JUnit 5 (`@Test`, `@BeforeEach`, `@AfterEach`).
- Quarkus modules use `@QuarkusTest`; standalone modules use plain JUnit 5.
- No `Thread.sleep()` in tests — use `Awaitility` for async assertions.
- Mocking: Mockito for unit tests; `@InjectMock` / `@QuarkusMock` for Quarkus integration tests.

## Error Handling

- Prefer checked exceptions for recoverable conditions at module boundaries.
- Use unchecked exceptions (`RuntimeException` subclasses) for programming errors.
- Never swallow exceptions silently; log at minimum `WARN` before rethrowing or recovering.
- Log with SLF4J via `infrastructure-component-log` — do not use `System.out` or `java.util.logging` directly.

## Javadoc

- All public interfaces and methods in `api/` packages must have Javadoc.
- Describe *what* the method does and any non-obvious constraints. Skip trivial getters.
- `@param` and `@return` tags are required when their meaning is not self-evident from the name.

## Dependency Rules

- Standalone modules: zero Quarkus dependencies (even optional/provided scope).
- Quarkus modules: may depend on standalone modules; use `implementation(enforcedPlatform(libs.quarkus.platform.bom))` for version alignment.
- Add dependencies to version catalogs (`libs.versions.toml`) rather than hardcoding versions in module `build.gradle.kts`.
