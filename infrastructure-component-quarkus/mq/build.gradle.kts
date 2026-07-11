dependencies {
    implementation(libs.bundles.share)
    implementation("io.quarkus:quarkus-kafka-client")
    api(project(":infrastructure-component-mq-api"))
    api(project(":infrastructure-component-mq-kafka"))
}
