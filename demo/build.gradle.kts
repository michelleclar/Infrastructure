plugins {
    java
    idea
    id("java-library")
    id("io.quarkus") version "3.19.3"
}

group = "org.carl"
version = "1.0-SNAPSHOT"

repositories {
    // NOTE: Save bandwidth
    mavenLocal()
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven {
        credentials {
            username = System.getenv("ALIYUN_MAVEN_USERNAME").toString()
            password = System.getenv("ALIYUN_MAVEN_PASSWORD").toString()
        }
        url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
    }
}
val mainSrc = "src/main/java"
val generatedDir = "src/main/generated"
sourceSets {
    main {
        java {
            srcDirs(generatedDir, mainSrc)
        }
    }
}

idea {
    val mainSrcFile = file("src/main/java")
    val generatedDirFile = file("src/main/generated")
    module {
        generatedSourceDirs.add(generatedDirFile)
        sourceDirs.add(generatedDirFile)
        sourceDirs.add(mainSrcFile)
    }
}

tasks.withType<Test>().configureEach {
    enabled = false
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    systemProperty("TESTCONTAINERS_REUSE_ENABLE", "true")
}
dependencies {
    implementation(enforcedPlatform(libs.quarkus.platform.bom))
    implementation(libs.bundles.all)
    implementation(libs.infrastructure.component.qdrant.grpc)
    implementation(libs.infrastructure.component.embedding.grpc)
    testImplementation(libs.bundles.test)
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.1.0")
}

quarkus {
    quarkusBuildProperties.put("quarkus.grpc.codegen.proto-directory", "./protos")
}

// NOTE: fix build pulsar
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.apache.bookkeeper:circe-checksum"))
            .using(module("org.apache.bookkeeper:circe-checksum:4.17.0"))
            .withoutClassifier()
        substitute(module("org.apache.bookkeeper:cpu-affinity"))
            .using(module("org.apache.bookkeeper:cpu-affinity:4.17.0"))
            .withoutClassifier()
    }
}
tasks.quarkusBuild {
    nativeArgs {
        "container-build" to true
        "builder-image" to "quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21"
    }
}