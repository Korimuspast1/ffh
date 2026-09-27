plugins {
    id("lexora.android.library")
    id("lexora.android.room")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.korimuspast1.lexora.core.database"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
