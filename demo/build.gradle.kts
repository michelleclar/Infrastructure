plugins {
    java
    idea
    id("io.quarkus") version "3.19.3"
}

group = "org.carl"
version = "1.0-SNAPSHOT"

repositories {
    // NOTE: Save bandwidth
    mavenLocal()
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven {
        credentials {
            username = findProperty("MAVEN_USERNAME").toString()
//            username =  "659e008740b4a9e2bd75af84"
            password = findProperty("MAVEN_PASSWORD").toString()
        }
        url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
    }
}
val mainSrc = "src/main/java"
val generatedDir = "src/main/generated"
sourceSets {
    named("main") {
        java {
            srcDir(generatedDir)
            srcDir(mainSrc)
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
    //    implementation libs.openfga
    implementation(enforcedPlatform(libs.quarkus.platform.bom))
    implementation(libs.bundles.all)
    testImplementation(libs.bundles.test)
}


//        var api = System.getenv("API_PREFIX") == null ? "/v1/api" : System.getenv("API_PREFIX")
quarkus {
    quarkusBuildProperties.put("quarkus.grpc.codegen.proto-directory", "./protos")
//            quarkusBuildProperties.put("quarkus.http.root-path", api)
}
tasks.quarkusBuild {
    nativeArgs {
        "container-build" to true
        "builder-image" to "quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21"

    }
}