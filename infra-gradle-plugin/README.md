# infra-gradle-plugin

发布 Gradle 约定插件 `org.carl.infra.quarkus`。插件内部应用仓库统一的 Quarkus Gradle 插件版本，并配置：

- `java-library` 与 `idea`
- Java 21 toolchain
- UTF-8 与 `-parameters`
- JUnit Platform 与 JBoss LogManager
- SNAPSHOT changing module 不缓存

业务项目只维护 `carl` 版本：

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        maven {
            credentials {
                username = System.getenv("ALIYUN_MAVEN_USERNAME") ?: ""
                password = System.getenv("ALIYUN_MAVEN_PASSWORD") ?: ""
            }
            url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
        }
        gradlePluginPortal()
    }
}
```

```toml
[versions]
carl = "1.0-BATE-SNAPSHOT"

[libraries]
infra-bom = { module = "org.carl:infra-bom", version.ref = "carl" }
infra-component-test = { module = "org.carl:infra-component-quarkus-test" }

[plugins]
infra-quarkus = { id = "org.carl.infra.quarkus", version.ref = "carl" }
```

```kotlin
plugins {
    alias(libs.plugins.infra.quarkus)
}

dependencies {
    implementation(enforcedPlatform(libs.infra.bom))
    testImplementation(libs.infra.component.test)
}
```

插件 marker 与实现从 Infra 的阿里云 Maven 仓库解析，内部 Quarkus 插件实现从
Gradle Plugin Portal 解析，因此两个仓库都必须保留。
