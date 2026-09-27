plugins {
    id("lexora.android.library")
    id("lexora.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.korimuspast1.lexora.data"
}

dependencies {
    implementation(project(":core:core-analytics"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-datastore"))
    implementation(project(":core:core-network"))
    implementation(project(":domain"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.retrofit.core)
    implementation(libs.timber)
}
