pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "infrastructure"
include("infrastructure-component")
include("infrastructure-component:persistence")
include("infrastructure-component:authorization")
include("infrastructure-component:search")
include("infrastructure-component:cache")
include("infrastructure-component:broadcast")
include("infrastructure-component:web")
include("infrastructure-component:discover")
include("infrastructure-component-qdrant-grpc")
include("infrastructure-component-embedding-grpc")
include("infrastructure-component-dto")
include("infrastructure-component:pulsar")
include("infrastructure-component:workflow")
include("infrastructure-component:metrics")
include("infrastructure-component-statemachine")
include("infrastructure-component-utils")
