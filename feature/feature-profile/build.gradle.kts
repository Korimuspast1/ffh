plugins {
    id("lexora.android.library")
    id("lexora.android.hilt")
}

android {
    namespace = "com.korimuspast1.lexora.feature.profile"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-design-system"))
    implementation(project(":core:core-ui"))
    implementation(project(":domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.coil.compose)
}
