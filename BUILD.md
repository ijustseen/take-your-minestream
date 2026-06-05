# Сборка и запуск

Проект использует [Stonecutter](https://stonecutter.kikugie.dev/): один исходный код, три версии Minecraft — **1.21.8**, **1.21.10**, **1.21.11**.

Требования: **JDK 21**, Gradle wrapper (`./gradlew`).

## Активная версия

Активная версия определяет, какой код попадает в `src/` и что запускают `runActive` / `buildActive`. Сейчас по умолчанию: **1.21.11** (`stonecutter.gradle.kts`).

Переключить активную версию:

```bash
# Через Gradle (рекомендуется)
./gradlew "Set active project to 1.21.11"
./gradlew "Set active project to 1.21.10"
./gradlew "Set active project to 1.21.8"

# Или вручную: stonecutter active "1.21.11" в stonecutter.gradle.kts
```

Перед коммитом (вернуть VCS-версию 1.21.8):

```bash
./gradlew "Reset active project"
```

## Сборка

```bash
# Все версии сразу; JAR-файлы в build/libs/<mod.version>/
./gradlew build buildAndCollect

# Только активная версия
./gradlew buildActive

# Конкретная версия
./gradlew :1.21.11:build :1.21.11:buildAndCollect
./gradlew :1.21.10:build :1.21.10:buildAndCollect
./gradlew :1.21.8:build :1.21.8:buildAndCollect
```

Имена артефактов: `tyms-<mod.version>+<minecraft>.jar` (например `tyms-1.4.1+1.21.11.jar`).

Проверка компиляции без полной сборки:

```bash
./gradlew :1.21.11:compileClientJava :1.21.8:compileClientJava
```

Очистка:

```bash
./gradlew clean
```

## Запуск клиента

Общая папка запуска для всех версий: `run/` в корне репозитория.

```bash
# Активная версия (сейчас 1.21.11)
./gradlew runActive

# Конкретная версия
./gradlew :1.21.11:runClient
./gradlew :1.21.10:runClient
./gradlew :1.21.8:runClient
```

## Структура версий

| Версия MC | Зависимости | Особенности |
|-----------|-------------|-------------|
| 1.21.8    | `versions/1.21.8/gradle.properties` | Override-исходники в `versions/1.21.8/src/client/java/` (старый GUI API) |
| 1.21.10   | `versions/1.21.10/gradle.properties` | Общий код из `src/` |
| 1.21.11   | `versions/1.21.11/gradle.properties` | Общий код из `src/`, активная по умолчанию |

Общие настройки мода: `gradle.properties` (`mod.version`, `mod.archives_name` и т.д.).

## Полезные Gradle-задачи

```bash
./gradlew tasks --group=project    # buildActive, runActive
./gradlew tasks --group=build      # build, buildAndCollect, clean
./gradlew tasks --group="Stonecutter tasks"   # переключение активной версии
```

## IDE

После смены активной версии обновите Gradle-проект в IDE. Задача `stonecutterIdea` подготавливает конфигурацию для IntelliJ.
