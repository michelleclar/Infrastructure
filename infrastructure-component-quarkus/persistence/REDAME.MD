## use

``` kotlin
dependencies {
implementation("org.carl:infrastructure-component-quarkus-persistence:1.0-BATE")
jooqGenerator("org.carl:infrastructure-component-persistence-jooq:1.0-BATE")

// build native and more plugins
implementation(enforcedPlatform(libs.quarkus.platform.bom))
// auto gen Q class
annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
```
