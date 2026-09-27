package com.korimuspast1.lexora.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.dagger.hilt.android")
        pluginManager.apply("com.google.devtools.ksp")

        dependencies.add("implementation", library("hilt-android"))
        dependencies.add("ksp", library("hilt-compiler"))
    }
}
