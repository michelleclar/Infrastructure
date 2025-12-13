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
    // SLF4J support
    implementation("org.slf4j:slf4j-api:2.0.9")

    // JBoss Logging support (provided by Quarkus)
    implementation("org.jboss.logging:jboss-logging:3.5.3.Final")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Logback for better logging format in tests
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}

tasks.test {
    useJUnitPlatform()
}
