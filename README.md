# Lexora

Lexora — Android-приложение для языкового обучения, создаваемое поэтапно в multi-module архитектуре на Kotlin, Jetpack Compose, Material 3, Hilt, Room, Retrofit и Coroutines.

Текущий статус:

- завершён **Этап 1 — архитектура и Gradle-структура**;
- добавлены **Этапы 2–3 — дизайн-система, кастомные иконки, маскот Nori и брендинг**;
- добавлены **Этапы 4–6 — Room-схема, Retrofit API-контракты и domain/use-case слой**;
- добавлен рабочий **Backend + Website** в `backend/`: JWT auth, курсы, уроки, прогресс, квесты, магазин, словарь, SRS и leaderboard API.

Документация:

- [`docs/architecture/stage-01.md`](docs/architecture/stage-01.md)
- [`docs/architecture/stage-02-03.md`](docs/architecture/stage-02-03.md)
- [`docs/architecture/stage-04-05-06.md`](docs/architecture/stage-04-05-06.md)

## Быстрый старт

Требования для сборки на локальной машине:

- JDK 17
- Android SDK с платформой Android 34
- сетевой доступ к Maven Central, Google Maven и Gradle distribution service

Команды:

```bash
./gradlew projects
./gradlew :app:assembleDevDebug
./gradlew :app:assembleProdRelease
```

Основная release-команда финального проекта:

```bash
./gradlew assembleRelease
```
