pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "infrastructure"
//quarkus module
include("infrastructure-component-quarkus")
include("infrastructure-component-quarkus:approval")
include("infrastructure-component-quarkus:mq")
include("infrastructure-component-quarkus:web")
include("infrastructure-component-quarkus:user")
include("infrastructure-component-quarkus:cache")
include("infrastructure-component-quarkus:search")
include("infrastructure-component-quarkus:metrics")
include("infrastructure-component-quarkus:workflow")
include("infrastructure-component-quarkus:discover")
include("infrastructure-component-quarkus:broadcast")
include("infrastructure-component-quarkus:persistence")
include("infrastructure-component-quarkus:authorization")
include("infrastructure-component-qdrant-grpc")
include("infrastructure-component-embedding-grpc")

// 'neat' module
include("infrastructure-component-dto")
include("infrastructure-component-log")
include("infrastructure-component-utils")

include("infrastructure-component-mq-api")
include("infrastructure-component-mq-pulsar")

include("infrastructure-component-persistence-jooq")
include("infrastructure-component-rule-engine")
include("infrastructure-component-pdp")
include("infrastructure-component-statemachine")
include("infrastructure-component-redis")
include("infrastructure-component-audit")
