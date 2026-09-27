pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Lexora"

include(":app")

include(":core:core-common")
include(":core:core-ui")
include(":core:core-network")
include(":core:core-database")
include(":core:core-datastore")
include(":core:core-design-system")
include(":core:core-analytics")

include(":domain")
include(":data")

include(":feature:feature-onboarding")
include(":feature:feature-auth")
include(":feature:feature-home")
include(":feature:feature-lesson")
include(":feature:feature-profile")
include(":feature:feature-leaderboard")
include(":feature:feature-quests")
include(":feature:feature-shop")
include(":feature:feature-practice")
include(":feature:feature-dictionary")
include(":feature:feature-achievements")
include(":feature:feature-settings")
include(":feature:feature-notifications")
