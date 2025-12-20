pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "infrastructure"
//quarkus module
include("infrastructure-component")
include("infrastructure-component:mq")
include("infrastructure-component:web")
include("infrastructure-component:user")
include("infrastructure-component:cache")
include("infrastructure-component:search")
include("infrastructure-component:metrics")
include("infrastructure-component:workflow")
include("infrastructure-component:discover")
include("infrastructure-component:broadcast")
include("infrastructure-component:persistence")
include("infrastructure-component:authorization")
include("infrastructure-component-qdrant-grpc")
include("infrastructure-component-embedding-grpc")

// 'neat' module
include("infrastructure-component-dto")
include("infrastructure-component-log")
include("infrastructure-component-utils")

include("infrastructure-component-mq-api")
include("infrastructure-component-mq-pulsar")

include("infrastructure-component-rule-engine")
include("infrastructure-component-pdp")
include("infrastructure-component-statemachine")
include("infrastructure-component-redis")
