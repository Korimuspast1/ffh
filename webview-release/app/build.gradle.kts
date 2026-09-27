plugins {
    id("com.android.application")
}

android {
    namespace = "com.korimuspast1.lexora"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.korimuspast1.lexora"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "0.5.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
