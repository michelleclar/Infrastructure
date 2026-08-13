# infra-component-quarkus-test

Quarkus 项目的统一测试依赖入口，当前传递提供：

- `io.quarkus:quarkus-flyway`
- `io.quarkus:quarkus-junit5`
- `io.rest-assured:rest-assured`
- `org.mockito:mockito-core`

消费方先以 `enforcedPlatform` 引入 `infra-bom`，再添加一个测试依赖：

```kotlin
dependencies {
    implementation(enforcedPlatform(libs.infra.bom))
    testImplementation(libs.infra.component.test)
}
```
