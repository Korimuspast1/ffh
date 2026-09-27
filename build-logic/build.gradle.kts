plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "lexora.android.application"
            implementationClass = "com.korimuspast1.lexora.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "lexora.android.library"
            implementationClass = "com.korimuspast1.lexora.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = "lexora.android.hilt"
            implementationClass = "com.korimuspast1.lexora.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "lexora.android.room"
            implementationClass = "com.korimuspast1.lexora.buildlogic.AndroidRoomConventionPlugin"
        }
        register("kotlinJvm") {
            id = "lexora.kotlin.jvm"
            implementationClass = "com.korimuspast1.lexora.buildlogic.KotlinJvmConventionPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
}
