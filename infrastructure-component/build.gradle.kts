plugins {
    id("org.kordamp.gradle.jandex") version "2.1.0"
    alias(libs.plugins.quarkus)
    id("maven-publish")
}
tasks.named("quarkusDependenciesBuild") {
    enabled = false
    dependsOn("jandex")
}
tasks.named("jandex") {
    enabled = false
}

subprojects {
    apply(plugin = "org.kordamp.gradle.jandex")
    apply(plugin = "maven-publish")
    apply(plugin = "io.quarkus")
//    tasks.named<Jar>("sourcesJar") {
//        dependsOn(tasks.named("compileQuarkusGeneratedSourcesJava"))
//    }

    tasks.named<JavaCompile>("compileJava") {
        dependsOn(tasks.named("compileQuarkusGeneratedSourcesJava"))
    }

    tasks.named("quarkusDependenciesBuild") {
        mustRunAfter(tasks.named("jandex"))
    }
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = project.group.toString()
                artifactId = parent?.name + "-" + project.name
                version = project.version.toString()
            }
        }
        repositories {

            maven {
                credentials {
                    username = System.getenv("ALIYUN_MAVEN_USERNAME").toString()
                    password = System.getenv("ALIYUN_MAVEN_PASSWORD").toString()
                }
                url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
            }

        }
    }


    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    }
    val libs = rootProject.libs
    dependencies {
        implementation(enforcedPlatform(libs.quarkus.platform.bom))
        testImplementation(libs.bundles.test)
        testImplementation(libs.bundles.web)
    }
}
