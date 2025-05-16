dependencies {
    api(libs.bundles.persistence)
    //-jakarta
    implementation(libs.querydsl.jpa)
    {
        artifact {
            classifier = "jakarta"
        }
    }
    implementation(libs.querydsl.sql) {
        artifact {
            classifier = "jakarta"
        }
    }
    implementation(libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }
}
