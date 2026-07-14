pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "infrastructure"
//quarkus module
include("infrastructure-component-quarkus")
include("infrastructure-component-quarkus:mq")
include("infrastructure-component-quarkus:web")
include("infrastructure-component-quarkus:cache")
include("infrastructure-component-quarkus:search")
include("infrastructure-component-quarkus:metrics")
include("infrastructure-component-quarkus:workflow")
include("infrastructure-component-quarkus:discover")
include("infrastructure-component-quarkus:persistence")
include("infrastructure-component-quarkus:authorization")
include("infrastructure-component-qdrant-grpc")
include("infrastructure-component-embedding-grpc")

// 'neat' module
include("infrastructure-component-dto")
include("infrastructure-component-log")
include("infrastructure-component-utils")
include("infrastructure-component-http")
include("infrastructure-component-web-api")
include("infrastructure-component-artifact-storage")

include("infrastructure-component-mq-api")
include("infrastructure-component-mq-pulsar")
include("infrastructure-component-mq-kafka")

include("infrastructure-component-workflow-core")
include("infrastructure-component-workflow-temporal")

include("infrastructure-component-persistence-jooq")
include("infrastructure-component-rule-engine")
include("infrastructure-component-statemachine")
include("infrastructure-component-redis")

// 平台/版本收口 BOM（java-platform）
include("infrastructure-bom")
