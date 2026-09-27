plugins {
    id("lexora.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.turbine)
}
