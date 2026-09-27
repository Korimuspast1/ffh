plugins {
    id("lexora.android.application")
    id("lexora.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.korimuspast1.lexora"

    defaultConfig {
        applicationId = "com.korimuspast1.lexora"
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            val configuredStore = providers.gradleProperty("LEXORA_RELEASE_STORE_FILE").orNull
            if (configuredStore != null) {
                storeFile = rootProject.file(configuredStore)
                storePassword = providers.gradleProperty("LEXORA_RELEASE_STORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("LEXORA_RELEASE_KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("LEXORA_RELEASE_KEY_PASSWORD").orNull
            } else {
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            val apiBaseUrl = providers.gradleProperty("LEXORA_API_BASE_URL").orElse("http://10.0.2.2:3000/api/v1/").get()
            val wsBaseUrl = providers.gradleProperty("LEXORA_WS_BASE_URL").orElse("ws://10.0.2.2:3000").get()
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "WS_BASE_URL", "\"$wsBaseUrl\"")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            val apiBaseUrl = providers.gradleProperty("LEXORA_STAGING_API_BASE_URL").orElse("https://staging.api.lexora.app/api/v1/").get()
            val wsBaseUrl = providers.gradleProperty("LEXORA_STAGING_WS_BASE_URL").orElse("wss://staging.api.lexora.app").get()
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "WS_BASE_URL", "\"$wsBaseUrl\"")
        }
        create("prod") {
            dimension = "environment"
            val apiBaseUrl = providers.gradleProperty("LEXORA_PROD_API_BASE_URL").orElse("https://api.lexora.app/api/v1/").get()
            val wsBaseUrl = providers.gradleProperty("LEXORA_PROD_WS_BASE_URL").orElse("wss://api.lexora.app").get()
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "WS_BASE_URL", "\"$wsBaseUrl\"")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(project(":core:core-analytics"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-datastore"))
    implementation(project(":core:core-design-system"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-ui"))
    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(project(":feature:feature-achievements"))
    implementation(project(":feature:feature-auth"))
    implementation(project(":feature:feature-dictionary"))
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-leaderboard"))
    implementation(project(":feature:feature-lesson"))
    implementation(project(":feature:feature-notifications"))
    implementation(project(":feature:feature-onboarding"))
    implementation(project(":feature:feature-practice"))
    implementation(project(":feature:feature-profile"))
    implementation(project(":feature:feature-quests"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-shop"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
