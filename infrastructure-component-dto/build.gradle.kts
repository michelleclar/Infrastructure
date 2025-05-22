plugins {
    id("maven-publish")
}
dependencies {
}

tasks.test {
    useJUnitPlatform()
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
