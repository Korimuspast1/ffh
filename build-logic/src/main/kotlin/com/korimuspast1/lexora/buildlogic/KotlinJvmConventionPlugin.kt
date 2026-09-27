package com.korimuspast1.lexora.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(17)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    "-Xjvm-default=all",
                    "-opt-in=kotlin.RequiresOptIn"
                )
            }
        }

        dependencies.add("implementation", library("kotlinx-coroutines-core"))
        dependencies.add("testImplementation", library("junit"))
        dependencies.add("testImplementation", library("mockk"))
        dependencies.add("testImplementation", library("kotlinx-coroutines-test"))
    }
}
