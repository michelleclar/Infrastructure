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
include("infrastructure-components:infrastructure-component-vector")
include("infrastructure-component-qdrant-grpc")
