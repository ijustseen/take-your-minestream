# Сборка и запуск

Проект использует [Stonecutter](https://stonecutter.kikugie.dev/): один исходный код, версии Minecraft — **1.21**, **1.21.1**, **1.21.4**, **1.21.8**, **1.21.10**, **1.21.11**, **26.1**.

Требования:
- **1.21.x** — JDK 21
- **26.1** — JDK 25 (Gradle toolchain подтянет автоматически, если установлен)
- Gradle 9.4+ (`./gradlew`)

## Активная версия

Активная версия определяет, какой код попадает в `src/` и что запускают `runActive` / `buildActive`. Сейчас по умолчанию: **1.21.11** (`stonecutter.gradle.kts`).

Переключить активную версию:

```bash
# Через Gradle (рекомендуется)
./gradlew "Set active project to 1.21.11"
./gradlew "Set active project to 1.21.10"
./gradlew "Set active project to 1.21.8"
./gradlew "Set active project to 26.1"

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
./gradlew :1.21:build :1.21:buildAndCollect
./gradlew :1.21.1:build :1.21.1:buildAndCollect
./gradlew :1.21.4:build :1.21.4:buildAndCollect
./gradlew :1.21.8:build :1.21.8:buildAndCollect
./gradlew :26.1:build :26.1:buildAndCollect
```

Имена артефактов: `tyms-<mod.version>+<minecraft>.jar` (например `tyms-1.4.3+1.21.11.jar`, `tyms-1.4.3+26.1.jar`).

### Minecraft 26.1 / 26.1.1 / 26.1.2

**26.1.1** и **26.1.2** — хотфиксы линии **26.1** (без ломающих изменений API). Отдельные Stonecutter-версии для них не нужны: собирается один JAR `+26.1`, в `fabric.mod.json` указано `>=26.1`, целевые клиенты перечислены в `versions/26.1/gradle.properties` (`mod.mc_targets=26.1 26.1.1 26.1.2`).

Особенности сборки 26.1:
- Отдельный скрипт `build-unobfuscated.gradle.kts` и плагин `net.fabricmc.fabric-loom` (без remapping)
- Minecraft **не обфусцирован** — официальные Mojang mappings, Java 25
- Порт с Yarn/1.21.11 на 26.1: замены в `stonecutter.gradle.kts` + overrides в `versions/26.1/src/`

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
./gradlew :26.1:runClient
```

## Структура версий

| Версия MC | Зависимости | Особенности |
|-----------|-------------|-------------|
| 1.21      | `versions/1.21/gradle.properties` | legacy GUI/HUD render + overrides из 1.21.8 |
| 1.21.1    | `versions/1.21.1/gradle.properties` | то же |
| 1.21.4    | `versions/1.21.4/gradle.properties` | RenderLayer GUI + overrides из 1.21.8 |
| 1.21.8    | `versions/1.21.8/gradle.properties` | `build-obfuscated.gradle.kts`, override в `versions/1.21.8/src/` |
| 1.21.10   | `versions/1.21.10/gradle.properties` | `build-obfuscated.gradle.kts`, общий `src/` |
| 1.21.11   | `versions/1.21.11/gradle.properties` | `build-obfuscated.gradle.kts`, активная по умолчанию |
| 26.1      | `versions/26.1/gradle.properties` | `build-unobfuscated.gradle.kts`, JAR для 26.1–26.1.2 |

Общие настройки мода: `gradle.properties` (`mod.version`, `mod.archives_name` и т.д.).

## Полезные Gradle-задачи

```bash
./gradlew tasks --group=project    # buildActive, runActive
./gradlew tasks --group=build      # build, buildAndCollect, clean
./gradlew tasks --group="Stonecutter tasks"   # переключение активной версии
```

## IDE

После смены активной версии обновите Gradle-проект в IDE. Задача `stonecutterIdea` подготавливает конфигурацию для IntelliJ.
