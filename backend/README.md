# Lexora Backend + Website

Рабочий API и сайт для Android-приложения Lexora.

## Запуск

```bash
cd backend
npm start
```

По умолчанию сервер слушает `0.0.0.0:3000`.

## Основные URL

- Website: `/`
- Healthcheck: `/api/v1/health`
- API config: `/api/v1/config`
- WebSocket leaderboard: `/ws/leaderboard`

## Реализованные REST endpoints

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `GET /api/v1/courses`
- `GET /api/v1/courses/:id/units`
- `GET /api/v1/lessons/:id`
- `POST /api/v1/lessons/:id/complete`
- `GET /api/v1/leaderboard/:leagueId`
- `GET /api/v1/quests/daily`
- `POST /api/v1/quests/:id/claim`
- `GET /api/v1/shop/items`
- `POST /api/v1/shop/buy/:itemId`
- `GET /api/v1/dictionary`
- `POST /api/v1/srs/review`
- `GET /api/v1/achievements`
- `GET /api/v1/friends`
- `POST /api/v1/friends/add/:userId`

## Данные

- Фейковых аккаунтов нет.
- Пользователь регистрируется сам.
- Пароль хранится как `scrypt` hash + salt.
- Сессии — JWT access/refresh.
- Пользовательские данные сохраняются в `backend/storage/lexora-db.json`.
- Образовательный контент генерируется из собственных seed-данных: 12 языков, 300 уроков, 2400 упражнений, 6240 слов.

## Android connection

В Android Gradle dev flavor по умолчанию использует:

```properties
LEXORA_API_BASE_URL=http://10.0.2.2:3000/api/v1/
LEXORA_WS_BASE_URL=ws://10.0.2.2:3000
```

Для сборки APK под публичный backend передай Gradle properties:

```bash
./gradlew :app:assembleDevDebug \
  -PLEXORA_API_BASE_URL=https://your-domain.example/api/v1/ \
  -PLEXORA_WS_BASE_URL=wss://your-domain.example
```
