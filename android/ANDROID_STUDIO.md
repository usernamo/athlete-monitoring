# Android Studio: «Module not specified»

## 1. Откройте правильную папку

**File → Open** → выберите только:

`C:\Users\PC\athlete-monitoring\android`

Не открывайте родительскую папку `athlete-monitoring` (там нет модуля `app`).

## 2. Синхронизация Gradle

**File → Sync Project with Gradle Files**

Дождитесь окончания без ошибок.

## 3. Конфигурация запуска

**Run → Edit Configurations…**

- Конфигурация: **app**
- **Module:** `AthleteDiary.app.main` (или в списке просто **app**)
- Apply → OK

## 4. Запуск

Выберите эмулятор/телефон → зелёная кнопка **Run** (конфигурация **app**).

## Запуск без Android Studio (если Studio всё ещё ругается)

```powershell
cd C:\Users\PC\athlete-monitoring\android
.\gradlew.bat installDebug
```

APK установится на подключённый телефон/эмулятор с включённой отладкой по USB.

Сборка release APK:

```powershell
.\gradlew.bat assembleRelease
```

Файл: `app\build\outputs\apk\release\app-release.apk`
