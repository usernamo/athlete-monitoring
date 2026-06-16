# Деплой на Vercel — API + PostgreSQL онлайн

Приложение Android подключается к **REST API в интернете**. На Vercel размещается **API**; **база PostgreSQL** — через **Vercel Postgres** (Neon), в Marketplace Vercel.

> **Важно:** Vercel не запускает долгоживущий Docker/PostgreSQL на своих серверах. БД — отдельный managed-сервис (Neon), API — serverless-функция.

---

## Шаг 1. GitHub

Залейте проект на GitHub (если ещё не залит):

```powershell
cd C:\Users\user\Documents\athlete-monitoring
git add .
git commit -m "Vercel deploy"
git push
```

---

## Шаг 2. Проект на Vercel

1. Откройте [vercel.com](https://vercel.com) → войдите через GitHub.
2. **Add New…** → **Project** → выберите репозиторий `athlete-monitoring`.
3. **Framework Preset:** **Other** (не Next.js, не Static).
4. **Root Directory:** оставьте корень (`.`).
5. **Build Command:** оставьте пустым (управляется `vercel.json`).
6. **Output Directory:** оставьте **пустым** — у API-only проекта нет папки `public`.
7. Vercel подхватит `vercel.json` автоматически.
8. Пока **не деплойте** — сначала создайте базу (шаг 3).

> Если уже был неудачный деплой: **Settings → General → Framework Preset → Other**, **Output Directory** — очистите поле → **Redeploy**.

---

## Шаг 3. База данных (Neon — Serverless Postgres)

На экране **Create a database** отдельной кнопки «Postgres» нет — PostgreSQL создаётся через **Marketplace**:

1. Прокрутите до блока **Marketplace Database Providers**.
2. Нажмите **Create** у **Neon** (подпись: *Serverless Postgres*).
   - Альтернатива: **Supabase** (*Postgres backend*) — тоже подойдёт.
3. Авторизуйтесь в Neon (если спросит) → выберите регион (ближе к пользователям, например EU).
4. Имя БД, например: `athlete-db` → **Create** / **Continue**.
5. **Connect to Project** → выберите проект `athlete-monitoring`.
6. Vercel добавит переменные окружения в проект:
   - `POSTGRES_URL` — pooled (для API на Vercel)
   - `POSTGRES_URL_NON_POOLING` — для миграций с ПК

### Переменные окружения API

В **Settings → Environment Variables** добавьте:

| Key | Value |
|-----|--------|
| `DATABASE_DIR` | `database` |
| `DATABASE_SSL` | `true` |
| `SEED_50_SLAVES` | `1` *(если нужны 50 slave-спортсменов)* |

`POSTGRES_URL` Vercel проставит сам после подключения Storage.

---

## Шаг 4. Первый деплой

1. **Deploy** (или push в `main` — автодеплой).
2. Дождитесь **Ready**.
3. URL проекта, например: `https://athlete-monitoring.vercel.app`

### Проверка

Откройте в браузере:

```
https://ВАШ-ПРОЕКТ.vercel.app/health
```

Ожидается:

```json
{"status":"ok"}
```

Первый запрос после деплоя может занять **15–60 секунд** — выполняются SQL-миграции и создание тестовых пользователей.

---

## Шаг 5. (Рекомендуется) Настройка БД с компьютера

Если `/health` отвечает ошибкой или миграции не успели на serverless, выполните с ПК:

1. Vercel → **Storage** → ваша БД → **`.env.local` tab** или **Connect**.
2. Скопируйте **`POSTGRES_URL_NON_POOLING`** (или External connection string).
3. В `backend/.env`:

```env
DATABASE_URL=postgres://...скопированный URL...
DATABASE_SSL=true
DATABASE_DIR=../database
```

4. Запуск:

```powershell
cd backend
npm install
npm run setup:remote
```

С 50 slave-спортсменами:

```powershell
npm run setup:remote:slaves
```

---

## Шаг 6. Android APK с онлайн-API

В `android/local.properties`:

```properties
sdk.dir=C\:\\Users\\user\\AppData\\Local\\Android\\Sdk
DEMO_API_URL=https://ВАШ-ПРОЕКТ.vercel.app/
```

Сборка:

```powershell
cd android
.\gradlew.bat assembleRelease
```

APK: `android\app\build\outputs\apk\release\app-release.apk`

На экране входа: **URL API** → **Проверить** → **Войти**.

---

## Тестовые аккаунты

| Роль | Email | Пароль |
|------|-------|--------|
| Спортсмен (м) | athlete-male@test.local | test123 |
| Спортсмен (ж) | athlete-female@test.local | test123 |
| Тренер (Master) | coach@test.local | test123 |
| Slave 1–50 | slave-01@test.local … slave-50@test.local | test123 |

---

## Частые проблемы

| Симптом | Решение |
|---------|---------|
| `database unavailable` | Проверьте Storage подключён к проекту; `POSTGRES_URL` в Environment |
| Долгий первый запрос | Нормально — миграции; затем `npm run setup:remote` с ПК |
| `database folder not found` | Добавьте `DATABASE_DIR=database` в Environment Variables |
| `No Output Directory named "public"` | Settings → Output Directory — **очистите**; Framework Preset → **Other**; Redeploy |
| Android не подключается | URL со слэшем в конце: `https://....vercel.app/` |
| Таймаут 60 с | Hobby: лимит 10 с на функцию — используйте `setup:remote` с ПК |

---

## Альтернатива: Render

Если Vercel даёт таймауты на миграциях, в проекте уже есть готовый деплой на Render: [RENDER_DEPLOY.md](RENDER_DEPLOY.md) + `render.yaml` (API + PostgreSQL в одном Blueprint).

---

## Обновление после изменений в коде

```powershell
git add .
git commit -m "update"
git push
```

Vercel пересоберёт проект автоматически.
