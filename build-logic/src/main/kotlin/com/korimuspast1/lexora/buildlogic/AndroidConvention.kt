package com.korimuspast1.lexora.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = versionInt("androidCompileSdk")

        defaultConfig {
            minSdk = versionInt("androidMinSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables.useSupportLibrary = true
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        buildFeatures {
            compose = true
            buildConfig = true
        }

        packaging {
            resources {
                excludes += setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE-notice.md",
                    "META-INF/DEPENDENCIES"
                )
            }
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }

    dependencies.add("coreLibraryDesugaring", library("android-desugarJdkLibs"))
    dependencies.add("testImplementation", library("junit"))
    dependencies.add("testImplementation", library("mockk"))
    dependencies.add("testImplementation", library("kotlinx-coroutines-test"))
    dependencies.add("androidTestImplementation", library("androidx-junit"))
    dependencies.add("androidTestImplementation", library("androidx-espresso-core"))
}
