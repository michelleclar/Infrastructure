pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "infrastructure"
include("infrastructure-components")
include("infrastructure-components:infrastructure-component-persistence")
//include "infrastructure-components:infrastructure-component-boot"
include("infrastructure-components:infrastructure-component-authorization")
include("infrastructure-components:infrastructure-component-search")
include("infrastructure-components:infrastructure-component-cache")
include("infrastructure-components:infrastructure-component-broadcast")
include("infrastructure-components:infrastructure-component-tool")
include("infrastructure-components:infrastructure-component-web")
include("infrastructure-components:infrastructure-component-discover")
include("infrastructure-component-qdrant-grpc")
include("infrastructure-component-embedding-grpc")
include("infrastructure-component-dto")
include("infrastructure-components:infrastructure-components-pulsar")
include("infrastructure-components:infrastructure-components-workflow")