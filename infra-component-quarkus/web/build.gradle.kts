dependencies {
    api(libs.bundles.web)
    implementation(project(":infra-component-utils"))
    testImplementation(project(":infra-component-quarkus:authorization"))
}
