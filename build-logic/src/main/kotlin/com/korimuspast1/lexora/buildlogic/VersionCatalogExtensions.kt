package com.korimuspast1.lexora.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.versionInt(alias: String): Int =
    libs.findVersion(alias).get().requiredVersion.toInt()

internal fun Project.library(alias: String): Provider<MinimalExternalModuleDependency> =
    libs.findLibrary(alias).get()
