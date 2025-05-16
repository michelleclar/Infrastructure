import com.google.protobuf.gradle.*

plugins {
    java
    id("maven-publish")
    id("com.google.protobuf") version "0.9.5"
}
dependencies {
    api(libs.quarkus.grpc)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.vertx:vertx-junit5:4.5.13")
    protobuf(files("dil/protos"))
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
protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
//        id("grpc") {
//            artifact = "io.grpc:protoc-gen-grpc-java:1.69.1"
//        }

        id("vertx-grpc") {
            artifact = "io.vertx:vertx-grpc-protoc-plugin2:4.5.13"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                named("java") {}
            }
            task.plugins {
                id("vertx-grpc")

            }
        }
    }
}