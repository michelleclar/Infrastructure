plugins {
    id("org.kordamp.gradle.jandex") version "2.1.0"
    alias(libs.plugins.quarkus)
    id("maven-publish")
}
tasks.named("quarkusDependenciesBuild") {
    enabled = false
    dependsOn("jandex")
}

subprojects {
    apply {
        plugin("org.kordamp.gradle.jandex")
        plugin("maven-publish")
        plugin("io.quarkus")
    }

    tasks.named("quarkusDependenciesBuild") {
        dependsOn("jandex")
    }
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
        repositories {

            maven {
                url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
                credentials {
                    username = findProperty("ALIYUN_MAVEN_USERNAME").toString()
                    password = findProperty("ALIYUN_MAVEN_password").toString()
                }
            }
        }
    }

    val libs = rootProject.libs
    dependencies {
        implementation(enforcedPlatform(libs.quarkus.platform.bom))
        testImplementation(libs.bundles.test)
        testImplementation(libs.bundles.web)
    }
}
