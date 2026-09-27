plugins {
    id("lexora.android.library")
}

android {
    namespace = "com.korimuspast1.lexora.core.analytics"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.timber)
}
