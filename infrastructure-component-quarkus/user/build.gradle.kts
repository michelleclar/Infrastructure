dependencies {
//    implementation(project(":infrastructure-component-utils"))
    implementation(project(":infrastructure-component-quarkus:web"))
    implementation(project(":infrastructure-component-quarkus:authorization"))
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}
