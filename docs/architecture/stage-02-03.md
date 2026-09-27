# Этапы 2–3 — дизайн-система, иконки, маскот и брендинг

## Название

Выбрано название **Lexora**: сочетает `lexis` (слово) и `aura` (персональная траектория обучения). Название короткое, не копирует Duolingo и подходит для мультиязычного продукта.

## Дизайн-система

Модуль: `core/core-design-system`.

Создано:

- `LexoraPalette` — собственная палитра:
  - primary
  - secondary
  - tertiary
  - success
  - warning
  - error
  - neutral
- У каждой группы есть 10 тонов.
- `LexoraExtendedColors` — дополнительные семантические цвета:
  - success / warning containers
  - legendary
  - gold
  - gem
  - heart
  - streak
- `LexoraTheme`:
  - light theme
  - dark theme
  - Dynamic Color на Android 12+ с fallback
  - Material 3 ColorScheme
  - Shapes по 8dp grid
  - Typography: 6+ размеров, 3+ веса
- `LexoraSpacing` — единая шкала отступов.
- `LexoraMotion` — duration/easing specs для дальнейших экранных переходов.

## Кастомные иконки

Модуль: `core/core-design-system`.

Файл: `LexoraIcons.kt`.

Все иконки нарисованы вручную через `ImageVector` path commands, без сторонних картинок и без emoji:

- Heart
- BrokenHeart
- Gem
- Flame
- Trophy
- Crown
- Shield
- Star
- Check
- Close
- Lock
- Paw
- Lightning
- Book
- Mic
- Speaker
- Key
- Flag
- User

## UI-компоненты

Модуль: `core/core-ui`.

Созданы переиспользуемые Compose-компоненты:

- `LingoButton`
- `LingoCard`
- `LingoModal`
- `LingoToast`
- `LingoProgressBar`
- `LingoAvatar`
- `LingoBadge`
- `LingoChip`
- `LingoDropdown`
- `LingoTabs`
- `LingoSkeleton`
- `LingoSpinner`
- `LingoBottomBar`
- `LingoTopBar`
- `LingoPathNode`
- `LingoConnector`
- `LingoHeartRow`
- `LingoStreakFlame`

## Маскот

Маскот: **Nori**, дружелюбный аксолотль-наставник.

Причина выбора: аксолотль не пересекается с совой Duolingo, хорошо работает как мягкий образовательный персонаж, легко анимируется через жабры, глаза, прыжки и эмоции.

Модуль: `core/core-ui`.

Файл: `LexoraMascot.kt`.

Маскот полностью рисуется через Compose Canvas. Состояния:

- idle
- happy
- sad
- celebrating
- thinking
- sleeping
- jumping
- waving
- crying
- level-up
- streak-fire
- cheering

## Брендинг Android

Добавлено:

- foreground vector
- round vector
- monochrome vector
- adaptive icon XML
- launcher background color
- Hilt `LexoraApplication`
- `MainActivity` подключена к `LexoraTheme`
- preview-экран использует дизайн-систему, path node, top bar, bottom bar и Nori mascot

## Следующий блок без остановки

Дальше реализуются:

1. Room schema: entities, DAO, migrations, converters.
2. Network layer: Retrofit API, DTO, auth interceptor, websocket contracts.
3. Domain models/use cases/repository contracts.
4. DI modules.
5. Навигация и полноценные feature-экраны.
