dependencies {
    implementation(libs.bundles.metrics)
}

tasks.matching { it.name == "quarkusGenerateCode" }.configureEach {
    doFirst {
        layout.buildDirectory.dir("classes/java/main").get().asFile.mkdirs()
    }
}
