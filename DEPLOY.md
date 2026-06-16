# Демо для оценки на другом телефоне

Нужны два шага: **сервер в интернете** (API + PostgreSQL) и **APK** с адресом этого сервера.

---

## Вариант A — Vercel (API + Vercel Postgres)

**Подробная инструкция:** [VERCEL_DEPLOY.md](VERCEL_DEPLOY.md)

Кратко:

1. Залейте проект на GitHub.
2. [vercel.com](https://vercel.com) → **New Project** → репозиторий.
3. **Storage** → **Postgres** → подключить к проекту.
4. Deploy → URL: `https://ваш-проект.vercel.app`
5. Проверка: `/health` → `{"status":"ok"}`

---

## Вариант B — Render.com (рекомендуется, если Vercel таймаутит миграции)

**Подробная инструкция:** [RENDER_DEPLOY.md](RENDER_DEPLOY.md)

Кратко:

1. Залейте проект на GitHub (`scripts/push-to-github.ps1` или вручную).
2. [render.com](https://render.com) → **New** → **Blueprint** → репозиторий с `render.yaml`.
3. Дождитесь **Live**. URL: `https://athlete-monitoring-api.onrender.com` (ваш будет свой).
4. Проверка: `https://ВАШ-URL.onrender.com/health` → `{"status":"ok"}`.

### Сборка APK для проверяющего

В `android/local.properties` (создайте, если нет):

```properties
DEMO_API_URL=https://ВАШ-URL.onrender.com/
```

Соберите release APK:

```powershell
cd C:\Users\PC\athlete-monitoring\android
.\gradlew.bat assembleRelease
```

APK: `android/app/build/outputs/apk/release/app-release.apk`

Передайте проверяющему **APK** и кратко:

- Установить APK (разрешить установку из неизвестных источников).
- На экране входа в поле **URL API** уже может быть ваш адрес; нажать **Проверить** → «Сервер доступен».
- Войти: `athlete-male@test.local` / `test123` (или `coach@test.local` для тренера).

> На бесплатном Render сервис «засыпает»; первый запрос после простоя может занять ~30 с.

---

## Вариант C — ваш ПК в Wi‑Fi (без облака)

Телефон проверяющего должен быть в **той же Wi‑Fi**, что и ваш компьютер.

```powershell
cd C:\Users\PC\athlete-monitoring
docker compose -f docker-compose.deploy.yml up -d --build
```

Узнайте IP ПК:

```powershell
.\scripts\show-lan-url.ps1
```

На телефоне в приложении укажите, например: `http://192.168.1.15:3000/` → **Проверить** → **Войти**.

Откройте в брандмауэре Windows **порт 3000** (входящие TCP).

---

## Вариант D — только backend на ПК (без Docker API)

```powershell
docker compose up -d
cd backend
npm install
npm start
```

API слушает `0.0.0.0:3000`. URL для телефона: `http://IP-ПК:3000/`.

---

## Тестовые аккаунты

| Роль | Email | Пароль |
|------|-------|--------|
| Спортсмен (м) | athlete-male@test.local | test123 |
| Спортсмен (ж) | athlete-female@test.local | test123 |
| Тренер | coach@test.local | test123 |

---

## Что передать проверяющему

1. Файл `app-release.apk`
2. Адрес API (если не зашит в `DEMO_API_URL`): `https://....onrender.com/`
3. Логины из таблицы выше

На экране входа можно **изменить URL** и нажать **Сохранить** — значение запоминается в телефоне.
