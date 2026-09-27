# Этап 1 — архитектура и Gradle-структура Lexora

Lexora — мобильное приложение для языкового обучения с чистой модульной архитектурой. Проект подготовлен как Android multi-module repository: каждый функциональный блок изолирован, зависимости направлены от UI к domain, а инфраструктура вынесена в core и data.

## Слои

- `app` — точка входа Android-приложения, сборочные варианты, permissions, signing, R8.
- `feature/*` — независимые Compose-фичи: onboarding, auth, lesson, leaderboard, shop, profile и другие экраны.
- `domain` — чистые модели, use case и контракты репозиториев без Android-зависимостей.
- `data` — реализации репозиториев, синхронизация Room/Network/DataStore.
- `core/*` — переиспользуемая инфраструктура: UI, design system, database, network, datastore, analytics, common utilities.
- `build-logic` — Gradle convention plugins для единых правил Kotlin, Android, Hilt и Room.

## Дерево проекта

```text
.
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── res/
├── build-logic/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/kotlin/com/korimuspast1/lexora/buildlogic/
├── core/
│   ├── core-analytics/
│   ├── core-common/
│   ├── core-database/
│   ├── core-datastore/
│   ├── core-design-system/
│   ├── core-network/
│   └── core-ui/
├── data/
├── domain/
├── feature/
│   ├── feature-achievements/
│   ├── feature-auth/
│   ├── feature-dictionary/
│   ├── feature-home/
│   ├── feature-leaderboard/
│   ├── feature-lesson/
│   ├── feature-notifications/
│   ├── feature-onboarding/
│   ├── feature-practice/
│   ├── feature-profile/
│   ├── feature-quests/
│   ├── feature-settings/
│   └── feature-shop/
├── gradle/libs.versions.toml
├── gradle/wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

## Gradle-конфигурация

- Kotlin DSL во всех Gradle-файлах.
- Version Catalog: `gradle/libs.versions.toml`.
- Convention plugins:
  - `lexora.android.application`
  - `lexora.android.library`
  - `lexora.android.hilt`
  - `lexora.android.room`
  - `lexora.kotlin.jvm`
- Android SDK: `minSdk 24`, `targetSdk 34`, `compileSdk 34`.
- Kotlin/JVM target: Java 17.
- Product flavors: `dev`, `staging`, `prod`.
- Build types: `debug`, `release`, `benchmark`.
- R8/ProGuard включён для release.
- Release signing берёт параметры из Gradle properties, а при локальной разработке использует debug signing config.

## Основные зависимости, подготовленные для следующих этапов

- UI: Jetpack Compose, Material 3, Navigation Compose, Lifecycle Compose.
- DI: Hilt + KSP.
- Data: Room, DataStore Preferences, Jetpack Security.
- Network: Retrofit, OkHttp, Kotlinx Serialization, Socket.IO.
- Media: Media3 ExoPlayer, Coil Compose, Lottie Compose.
- Android capabilities: SplashScreen API, WorkManager, Credential Manager, BiometricPrompt.
- Firebase: Analytics, Crashlytics, Messaging через Firebase BoM.
- Tests: JUnit, Mockk, Coroutines Test, Compose UI Test, Espresso.

## Сборочные команды

```bash
./gradlew projects
./gradlew :app:assembleDevDebug
./gradlew :app:assembleProdRelease
./gradlew assembleRelease
```

Для production-подписи можно передать свойства Gradle:

```properties
LEXORA_RELEASE_STORE_FILE=keystore/lexora-release.jks
LEXORA_RELEASE_STORE_PASSWORD=changeit
LEXORA_RELEASE_KEY_ALIAS=lexora
LEXORA_RELEASE_KEY_PASSWORD=changeit
```
