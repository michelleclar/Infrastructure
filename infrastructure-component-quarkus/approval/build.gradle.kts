dependencies {
    implementation(libs.bundles.web)
    implementation(project(":infrastructure-component-quarkus:persistence"))
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
