plugins {
    id("lexora.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.korimuspast1.lexora.core.datastore"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
