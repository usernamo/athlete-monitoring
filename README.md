# Athlete Monitoring Platform (тестовый стенд)

Платформа мониторинга спортсменов: гибкие метрики (`metric_type` + `value`), PostgreSQL, REST API, тестовое Android-приложение.

## Структура

```
athlete-monitoring/
├── database/          # SQL-схема (27 таблиц) + тестовые данные
├── docker-compose.yml # PostgreSQL 16
├── backend/           # Node.js REST API
├── android/           # Kotlin + Jetpack Compose
├── api/               # Vercel serverless entry
├── vercel.json        # Конфиг деплоя на Vercel
└── scripts/           # Импорт из Excel + анализ файлов
```

**Онлайн-деплой:** [VERCEL_DEPLOY.md](VERCEL_DEPLOY.md) (Vercel + Postgres) или [RENDER_DEPLOY.md](RENDER_DEPLOY.md) (Render Blueprint).

## 1. База данных (PostgreSQL)

```powershell
cd C:\Users\PC\athlete-monitoring
docker compose up -d
```

Параметры:

| Параметр | Значение |
|----------|----------|
| Host | `localhost:5432` |
| DB | `athlete_monitoring` |
| User / Password | `athlete` / `athlete_secret` |

Схема и seed применяются автоматически из `database/001_schema.sql` и `002_seed.sql`.

### Тестовый пользователь

| Пользователь | Email | Пароль | Пол |
|--------------|-------|--------|-----|
| Мужчина | `athlete-male@test.local` | `test123` | male |
| Женщина | `athlete-female@test.local` | `test123` | female (+ вкладка «Цикл») |

Регистрация: вкладка **Регистрация** — имя, фамилия, возраст, пол, рост, вес, окружность груди.

## 2. Backend API

```powershell
cd backend
copy .env.example .env
npm install
npm start
```

API: `http://localhost:3000`

Основные эндпоинты:

- `POST /api/auth/login`
- `GET /api/athletes/:id/dashboard`
- `GET /api/metric-types`
- `POST /api/athletes/:id/metrics`
- `POST /api/athletes/:id/daily-reports`
- `GET /api/athletes/:id/nutrition`

## 3. Импорт из Excel (дневник спортсмена)

Файлы (из Telegram Desktop):

- `дневник спортсмена.xlsx`
- `дневник спортсмена 1.xlsx`
- `дневник спортсмена 2.xlsx`

```powershell
pip install -r scripts\requirements.txt
python scripts\import_excel.py
```

Скрипт ищет колонки вроде «качество сна», «мотивация», «усталость», «пульс покоя» и записывает их в `athlete_metrics` через `metric_types`.

Анализ структуры Excel/DOCX:

```powershell
python C:\Users\PC\.cursor\projects\empty-window\analyze_sources.py
```

## 4. Android-приложение

1. Установите [Android Studio](https://developer.android.com/studio).
2. **File → Open** → папка `C:\Users\PC\athlete-monitoring\android`
3. Дождитесь Gradle Sync.
4. Запустите эмулятор (API 26+).
5. Убедитесь, что API запущен (`npm start` в `backend`).

Эмулятор обращается к ПК как `10.0.2.2` — в `app/build.gradle.kts` уже задано:

`http://10.0.2.2:3000/`

Для **оценки на другом телефоне** (удалённо) см. **[DEPLOY.md](DEPLOY.md)** — деплой на Render или Wi‑Fi + настраиваемый URL на экране входа.

Для **физического телефона** в той же сети: `http://IP-ПК:3000/` или укажите URL на экране входа в приложении.

### Экраны приложения

- Вход (тестовый аккаунт спортсмена)
- Главная: аналитика, метрики, тренировки, вода
- Метрики: добавление отдельного показателя
- **Дневник**: форма как строка Excel (дата, сон, пульс, вес, тренировка…) — одно сохранение на день
- **Питание**: приёмы пищи (№, аппетит, время, продукты, БЖУ, ккал), перекусы, **ИТОГО**, спортпит, фармподдержка, вода за сутки

## Дневник = ваша Excel-таблица

| Задача | Как |
|--------|-----|
| Внести данные как в Excel | Вкладка **Дневник** в приложении |
| Импорт старых Excel | `python scripts/import_excel.py` |
| Экспорт для сравнения | `python scripts/export_excel.py` → `exports/дневник_из_приложения.xlsx` |
| Подогнать колонки под ваш файл | `python scripts/sync_columns_from_excel.py "путь\к\файлу.xlsx"` |

Колонки настраиваются в `backend/src/excelForm.js` (подписи на русском = заголовки Excel).

## Архитектурные принципы

1. **Time-series** — `athlete_metrics.measured_at`, индексы по времени.
2. **Гибкость** — новые показатели через `metric_types`, без ALTER TABLE.
3. **Analytics-ready** — `analytics_snapshots` для AI/прогнозов.

## DOCX (общее описание)

Файл `Дневник спортсмена (общ).docx` — извлекается скриптом `analyze_sources.py` (нужны `python-docx`, `openpyxl`, `pandas`).

## Следующие шаги

- Подключить реальные листы Excel после `analyze_sources.py` (уточнить `METRIC_ALIASES` в `import_excel.py`)
- JWT вместо test-token
- Офлайн-кэш (Room) на Android
- Роли coach / doctor в UI
