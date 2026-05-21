# Деплой на Render — пошагово

Нужен аккаунт [render.com](https://render.com) (можно через GitHub).

---

## Шаг 1. Загрузить код на GitHub

Если репозитория ещё нет, в PowerShell:

```powershell
cd C:\Users\PC\athlete-monitoring

git init
git add .
git commit -m "Athlete monitoring demo for Render"

# Создайте пустой репозиторий на github.com (без README), затем:
git remote add origin https://github.com/ВАШ_ЛОГИН/athlete-monitoring.git
git branch -M main
git push -u origin main
```

> В `.gitignore` уже исключены `node_modules`, `.env`, `android/build` — это нормально.

---

## Шаг 2. Blueprint на Render

1. Откройте [dashboard.render.com](https://dashboard.render.com)
2. **New +** → **Blueprint**
3. Подключите GitHub и выберите репозиторий `athlete-monitoring`
4. Render найдёт файл `render.yaml` в корне — нажмите **Apply**

Создадутся два ресурса:

| Ресурс | Назначение |
|--------|------------|
| **athlete-db** | PostgreSQL |
| **athlete-monitoring-api** | Node.js API |

Дождитесь статуса **Live** (первый деплой 5–15 минут).

---

## Шаг 3. Проверка API

1. Откройте сервис **athlete-monitoring-api** в Dashboard
2. Скопируйте URL вверху, например:  
   `https://athlete-monitoring-api.onrender.com`
3. В браузере откройте:  
   `https://athlete-monitoring-api.onrender.com/health`  

Ожидаемый ответ:

```json
{"status":"ok"}
```

4. (Опционально) логи: вкладка **Logs** — должны быть строки:
   - `[migrate] applying ... SQL files`
   - `[setup] Test users: athlete-male@test.local ...`
   - `Server is RUNNING`

---

## Шаг 4. Настроить Android APK

Создайте файл `android/local.properties`:

```properties
sdk.dir=C\:\\Users\\PC\\AppData\\Local\\Android\\Sdk
DEMO_API_URL=https://athlete-monitoring-api.onrender.com/
```

Замените URL на **ваш** из Render (со слэшем в конце).

Сборка:

```powershell
cd C:\Users\PC\athlete-monitoring\android
.\gradlew.bat assembleRelease
```

APK: `app\build\outputs\apk\release\app-release.apk`

---

## Шаг 5. Что передать на оценку

1. APK  
2. Адрес API: `https://ваш-сервис.onrender.com/`  
3. Логины:

| Роль | Email | Пароль |
|------|-------|--------|
| Спортсмен | athlete-male@test.local | test123 |
| Спортсмен (ж) | athlete-female@test.local | test123 |
| Тренер | coach@test.local | test123 |

На экране входа: вставить URL → **Проверить** → **Войти**.

> **Важно:** на бесплатном тарифе сервис «засыпает». Первый запрос после простоя может идти 30–60 секунд — это нормально.

---

## Если деплой упал (Failed deploy)

### 1. Откройте Logs сервиса API

Частые причины:

| Ошибка в логах | Решение |
|----------------|---------|
| `database folder not found` | В репозитории должна быть папка `database/` в корне (рядом с `backend/`) |
| `Database: FAILED` | Подождите, пока БД станет **Available**; перезапустите API (**Manual Deploy**) |
| `ECONNREFUSED` к Postgres | Проверьте, что `DATABASE_URL` привязан к БД (Blueprint делает это сам) |
| Ошибка при `002_seed.sql` | **Manual Deploy** → Redeploy; при повторе напишите текст ошибки |

### 2. Ручной деплой без Blueprint

**База данных**

- **New +** → **PostgreSQL** → имя `athlete-db` → Create  
- Скопируйте **Internal Database URL**

**Web-сервис**

- **New +** → **Web Service** → репозиторий  
- **Root Directory:** оставить пустым (корень репо)  
- **Build Command:** `cd backend && npm install`  
- **Start Command:** `cd backend && npm start`  
- **Environment:**

| Key | Value |
|-----|--------|
| `NODE_ENV` | `production` |
| `HOST` | `0.0.0.0` |
| `DATABASE_DIR` | `../database` |
| `DATABASE_URL` | вставить Internal Database URL |

- **Health Check Path:** `/health`

---

## Обновление после изменений в коде

```powershell
git add .
git commit -m "update"
git push
```

Render пересоберёт сервис автоматически.

---

## Локальная проверка перед Render

```powershell
cd C:\Users\PC\athlete-monitoring
docker compose up -d
cd backend
npm install
npm start
```

`http://localhost:3000/health` → `{"status":"ok"}`
