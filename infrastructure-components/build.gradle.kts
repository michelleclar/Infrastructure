plugins {
    id("org.kordamp.gradle.jandex") version "2.1.0"
    id("maven-publish")
}

subprojects {
    apply {
        plugin("org.kordamp.gradle.jandex")
        plugin("maven-publish")
    }

    val env: Map<String, String> = file(".env").takeIf { it.exists() }
        ?.readLines()
        ?.filterNot { it.trim().startsWith("#") || it.isBlank() }?.associate {
            val (k, v) = it.split("=", limit = 2)
            k.trim() to v.trim()
        } ?: emptyMap()

    fun Project.env(key: String): String? = env[key]

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
            //        maven {
            //            name = "GitHubPackages"
            //            url = uri("https://maven.pkg.github.com/michelleclar/Infrastructure")
            //            credentials {
            //                username = 'carl'
            //                password = System.getenv('GITHUB_TOKEN')
            //            }
            //        }
            maven {
                url = uri("https://packages.aliyun.com/659e01070cab697efe1345a8/maven/repo-wdhey")
                credentials {
                    username = "${project.env("MAVEN_USERNAME")}"
                    username = "${project.env("MAVEN_USERNAME")}"
                }
            }
        }
//            publications {
//                gpr(MavenPublication) {
//                    from(components.java)
//                }
//            }
    }

    tasks.named("quarkusDependenciesBuild") {
        dependsOn("jandex")
    }

    val libs = rootProject.libs
    dependencies {
        testImplementation(libs.bundles.web)
        if (!name.startsWith("infrastructure-component-tool")) {
            api(project(":infrastructure-components:infrastructure-component-tool"))
        }
    }
}
