# Kotlin Style Guide (Build Scripts)

> Kotlin 在本项目中仅用于 Gradle 构建脚本（`build.gradle.kts`, `settings.gradle.kts`）。
> 代码风格遵循 `kotlin.code.style=official`（Kotlin 官方风格）。

## Formatting

- Indent: 4 spaces
- Line length: 最大 120 字符
- 遵循 [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

## Build Script Conventions

- 使用 `libs` version catalog 管理依赖版本，避免硬编码版本号
- 插件通过 `alias(libs.plugins.xxx)` 引用，保持一致性
- 子模块配置放在父模块的 `subprojects {}` 块中

```kotlin
// 推荐：使用 version catalog
dependencies {
    implementation(libs.quarkus.core)
    testImplementation(libs.junit.jupiter)
}

// 不推荐：硬编码版本
dependencies {
    implementation("io.quarkus:quarkus-core:3.19.3")
}
```

## Publishing Configuration

- Maven 发布凭证通过环境变量传入，不硬编码
- `artifactId` 格式：`${project.parent?.name}-${project.name}`

```kotlin
credentials {
    username = System.getenv("ALIYUN_MAVEN_USERNAME").toString()
    password = System.getenv("ALIYUN_MAVEN_PASSWORD").toString()
}
```

## Comments in Build Scripts

- 仅在配置项有非显而易见的约束时添加注释
- 临时禁用的配置使用 `// NOTE:` 前缀说明原因
