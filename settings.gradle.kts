pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "infra"
//quarkus module
include("infra-component-quarkus")
include("infra-component-quarkus:mq")
include("infra-component-quarkus:mq-kafka")
include("infra-component-quarkus:mq-pulsar")
include("infra-component-quarkus:web")
include("infra-component-quarkus:cache")
include("infra-component-quarkus:search")
include("infra-component-quarkus:metrics")
include("infra-component-quarkus:workflow")
include("infra-component-quarkus:persistence")
include("infra-component-quarkus:authorization")
include("infra-component-quarkus:test")
include("infra-component-qdrant-grpc")
include("infra-component-embedding-grpc")

// 'neat' module
include("infra-component-dto")
include("infra-component-log")
include("infra-component-utils")
include("infra-component-http")
include("infra-component-web-api")
include("infra-component-artifact-storage")

include("infra-component-mq-api")
include("infra-component-mq-pulsar")
include("infra-component-mq-kafka")

include("infra-component-workflow-core")
include("infra-component-workflow-temporal")

include("infra-component-persistence-jooq")
include("infra-component-rule-engine")
include("infra-component-statemachine")
include("infra-component-redis")
include("infra-component-discover-api")
include("infra-component-discover-consul")

// Gradle 约定插件：统一应用并配置 Java/Quarkus 插件
include("infra-gradle-plugin")

// 平台/版本收口 BOM（java-platform）
include("infra-bom")
