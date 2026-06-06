import org.jooq.meta.kotlin.*

plugins {
    id("maven-publish")
    id("nu.studer.jooq") version "10.2.1"
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
    implementation(project(":infrastructure-component-persistence-jooq"))
    implementation(project(":infrastructure-component-workflow-core"))

    testImplementation(libs.temporal.testing)
    testRuntimeOnly(libs.postgresql)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // JOOQ generator dependencies
    jooqGenerator("org.postgresql:postgresql:42.7.4")
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




jooq {
    version.set("3.20.3")
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc {
                    driver = "org.postgresql.Driver"
                    url = System.getenv("ER_ROOL_META_JDBC_URL") ?: "jdbc:postgresql://180.184.66.147:31432/db"
                    user = System.getenv("ER_ROOL_META_DB_USERNAME") ?: "root"
                    password = System.getenv("ER_ROOL_META_DB_PASSWORD") ?: "root"
                }
                generator {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        forcedTypes {
                        }
                    }
                    generate {
                        isDeprecated = false
                        isRecords = true
                        isDaos = true
                        isFluentSetters = true
                    }
                    target {
                        packageName = "org.carl.ertool.infrastructure.jooq.generated"
                        directory = "src/main/generated"
                    }
                }
            }
        }
    }
}
