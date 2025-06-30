## build native
```kotlin
// NOTE: fix build pulsar
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.apache.bookkeeper:circe-checksum"))
            .using(module("org.apache.bookkeeper:circe-checksum:4.17.0"))
            .withoutClassifier()
        substitute(module("org.apache.bookkeeper:cpu-affinity"))
            .using(module("org.apache.bookkeeper:cpu-affinity:4.17.0"))
            .withoutClassifier()
    }
}
```