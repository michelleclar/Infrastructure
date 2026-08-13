plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("io.quarkus:gradle-application-plugin:${libs.versions.quarkus.get()}")
    testImplementation(platform(libs.quarkus.platform.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("infraQuarkus") {
            id = "org.carl.infra.quarkus"
            implementationClass = "org.carl.infra.gradle.InfraQuarkusPlugin"
            displayName = "Carl Infra Quarkus Convention Plugin"
            description = "Applies the Java library and Quarkus plugins with Infra conventions."
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("ALIYUN_MAVEN_USERNAME") ?: ""
                password = System.getenv("ALIYUN_MAVEN_PASSWORD") ?: ""
            }
            url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
        }
    }
}
