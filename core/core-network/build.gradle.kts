plugins {
    id("lexora.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.korimuspast1.lexora.core.network"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.socket.io.client)
    implementation(libs.timber)
}
