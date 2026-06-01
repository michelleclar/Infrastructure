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
                username = System.getenv("ALIYUN_MAVEN_USERNAME").toString()
                password = System.getenv("ALIYUN_MAVEN_PASSWORD").toString()
            }
            url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
        }
    }
}

dependencies {
    api(libs.temporal.sdk)
    implementation(project(":infrastructure-component-log"))

    testImplementation(libs.temporal.testing)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("org.jboss.logging.provider", "jdk")
    systemProperty(
        "java.util.logging.config.file",
        layout.projectDirectory.file("src/test/resources/logging.properties").asFile.absolutePath,
    )
    testLogging {
        showStandardStreams = true
    }
}
