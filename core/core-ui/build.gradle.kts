plugins {
    id("lexora.android.library")
}

android {
    namespace = "com.korimuspast1.lexora.core.ui"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-design-system"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
