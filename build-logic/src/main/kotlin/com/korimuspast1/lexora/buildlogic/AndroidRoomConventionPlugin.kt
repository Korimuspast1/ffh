package com.korimuspast1.lexora.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("androidx.room")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        dependencies.add("implementation", library("androidx-room-runtime"))
        dependencies.add("implementation", library("androidx-room-ktx"))
        dependencies.add("ksp", library("androidx-room-compiler"))
    }
}
