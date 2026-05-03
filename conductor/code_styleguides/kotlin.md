# Kotlin Style Guide

Applies to Kotlin files in this project — currently Gradle build scripts (`build.gradle.kts`, `settings.gradle.kts`).

## General

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) as the baseline
- `kotlin.code.style=official` is set in `gradle.properties` — IDEs should pick this up automatically

## Gradle Build Scripts

### Formatting

- **Indentation**: 4 spaces, no tabs
- **Line length**: 120 characters max
- Each `dependencies {}` block groups entries in this order:
  1. `api(...)` declarations
  2. `implementation(...)` declarations
  3. `testImplementation(...)` declarations
  4. Blank line separating each group

### Plugin Declarations

- Use the `plugins {}` block at the top of every `build.gradle.kts`
- Pin versions for non-BOM plugins explicitly: `id("some.plugin") version "x.y.z"`
- Do not apply plugins via the legacy `apply plugin: "..."` syntax

### Dependency Management

- Declare all shared versions in the version catalog (`gradle/libs.versions.toml`)
- Reference catalog entries via `libs.<alias>` — do not hardcode version strings in module build files
- Use `enforcedPlatform` only for the Quarkus BOM; avoid it elsewhere to prevent version conflicts
- Prefer `api(...)` for dependencies that are part of a module's public API; use `implementation(...)` for internal dependencies

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Variables / vals | lowerCamelCase | `quarkusVersion` |
| Extension functions | lowerCamelCase | `configureJavaToolchain()` |
| Constants (top-level) | UPPER_SNAKE_CASE | `DEFAULT_JVM_ARGS` |

### Task Configuration

- Use `tasks.named<TaskType>("taskName") { }` rather than `tasks.getByName(...)` to avoid eager resolution
- Disable tasks explicitly with `enabled = false` rather than removing them

### Publishing Configuration

- Maven 发布凭证通过环境变量传入，不硬编码

```kotlin
credentials {
    username = System.getenv("ALIYUN_MAVEN_USERNAME").toString()
    password = System.getenv("ALIYUN_MAVEN_PASSWORD").toString()
}
```

### Comments

- Write comments only when the build logic is non-obvious (e.g. why a workaround exists)
- Use `// NOTE:` prefix for temporarily disabled configuration — explain why
- Do not comment out code — remove it. Use git history if rollback is needed

## Version Catalog (`libs.versions.toml`)

- Group versions under `[versions]`, libraries under `[libraries]`, plugins under `[plugins]`
- Use kebab-case for alias names: `quarkus-platform-bom`, `infrastructure-component-redis`
- Keep the catalog sorted alphabetically within each section
