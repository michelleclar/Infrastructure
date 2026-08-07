plugins {
    id("maven-publish")
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
            credentials {
                username = System.getenv("ALIYUN_MAVEN_USERNAME") ?: ""
                password = System.getenv("ALIYUN_MAVEN_PASSWORD") ?: ""
            }
            url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
        }
    }
}

dependencies {
    api(project(":infra-component-discover-api"))
    implementation(libs.vertx.core)
    implementation(libs.vertx.consul.client)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:consul:1.21.4")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

tasks.test {
    useJUnitPlatform()
}

val verifyNoFrameworkRuntimeDependencies by tasks.registering {
    group = "verification"
    description = "Verifies that the standalone Consul client has no framework runtime dependencies."
    doLast {
        val forbiddenGroups =
            setOf(
                "io.quarkus",
                "io.quarkiverse.config",
                "io.smallrye.stork",
                "org.eclipse.microprofile.config",
            )
        val forbidden =
            configurations
                .getByName("runtimeClasspath")
                .resolvedConfiguration
                .resolvedArtifacts
                .map { "${it.moduleVersion.id.group}:${it.name}:${it.moduleVersion.id.version}" }
                .filter { dependency -> forbiddenGroups.any { dependency.startsWith("$it:") } }
        check(forbidden.isEmpty()) {
            "Standalone Consul client contains forbidden runtime dependencies: $forbidden"
        }
    }
}

tasks.named("check") {
    dependsOn(verifyNoFrameworkRuntimeDependencies)
}
