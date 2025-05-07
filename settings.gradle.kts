pluginManagement {
    repositories {

        mavenLocal()
//        maven { url = uri("https://maven.aliyun.com/repository/central") }
//        maven { url 'https://mirrors.cloud.tencent.com/repository/maven' }
//        maven { url = uri("https://maven.aliyun.com/repository/public") }
//        maven { url = uri("https://maven.aliyun.com/repository/google") }
//        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        //        maven { url 'https://maven.aliyun.com/repository/jcenter' }

//        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        //        maven { url 'https://maven.aliyun.com/repository/apache-snapshots' }
        gradlePluginPortal()
//        mavenCentral()
    }
}

rootProject.name = "scaffold"
include("infrastructure-components")
include("infrastructure-components:infrastructure-component-persistence")
//include "infrastructure-components:infrastructure-component-boot"
include("infrastructure-components:infrastructure-component-authorization")
include("infrastructure-components:infrastructure-component-search")
include("infrastructure-components:infrastructure-component-cache")
include("infrastructure-components:infrastructure-component-broadcast")
include("infrastructure-components:infrastructure-component-tool")
include("infrastructure-components:infrastructure-component-web")
include("infrastructure-components:infrastructure-components-discover")

