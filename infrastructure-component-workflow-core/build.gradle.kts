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
    implementation(project(":infrastructure-component-log"))

    // Jackson for JSON ser/de of WorkflowDefinition
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.18.0"))
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Jakarta EL (condition expressions)
    implementation("jakarta.el:jakarta.el-api:5.0.1")
    implementation("org.glassfish:jakarta.el:5.0.0-M1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
