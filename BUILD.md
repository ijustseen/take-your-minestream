# Сборка и запуск

## Идея раскладки

```
core/                         # общий код + ассеты (чаты, UI, lang, textures)
fabric|forge|neoforge/
  src → ../core               # симлинк для Stonecutter
  versions/{mc}/              # overrides и gradle.properties версии
  build-*.gradle.kts → ../…   # скрипт лоадера
```

- **core** — один исходник: подключения чатов, рендер, конфиг, `assets/take-your-stream-chat/`, `pack.mcmeta`, FML `mods.toml`
- **fabric / forge / neoforge** — только лоадерный entrypoint, ClientHooks и MC-overrides
- Namespace ресурсов / Fabric id: `take-your-stream-chat`
- FML modId: `take_your_stream_chat` (дефисы Forge не принимает)

Fabric Loom и Architectury Loom **нельзя** remap’ить в одном Gradle-прогоне (`JarManifestService`). Поэтому сборка JAR — **отдельная команда на лоадер** (изолированный `-Pmod.loaders=…`).

| Лоадер | Версии Minecraft |
|--------|------------------|
| **Fabric** | 1.20.1, 1.21, 1.21.1, 1.21.4, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.2, 26.3 |
| **Forge** | 1.20.1 |
| **NeoForge** | 1.21.1 |

Требования: Java 17 (1.20.1), 21 (1.21.x), 25 (26.x); Gradle 9.6+.

## Сборка JAR (по лоадерам)

Артефакты → `build/libs/<mod.version>/{fabric,forge,neoforge}/`.

```bash
# Только Fabric (все MC-версии ветки)
./gradlew buildAndCollectFabric

# Только Forge 1.20.1
./gradlew buildAndCollectForge

# Только NeoForge 1.21.1
./gradlew buildAndCollectNeoForge

# Все три лоадера подряд (три изолированных прогона)
./gradlew buildAndCollectAll
# алиас:
./gradlew buildAndCollect
```

Пример:

```
build/libs/2.2.2/
  fabric/tysc-2.2.2+1.21.11.jar
  forge/tysc-2.2.2+1.20.1-forge.jar
  neoforge/tysc-2.2.2+1.21.1-neoforge.jar
```

Одна версия вручную:

```bash
./gradlew -Pmod.loaders=fabric :fabric:1.21.11:collectJars
./gradlew -Pmod.loaders=forge :forge:1.20.1:collectJars
./gradlew -Pmod.loaders=neoforge :neoforge:1.21.1:collectJars
```

## Запуск клиента

По умолчанию в settings подключены все лоадеры (удобно для IDE). Run-каталоги: `run/`, `run-forge/`, `run-neoforge/`.

```bash
./gradlew runActive                          # активная Stonecutter-версия (fabric 1.21.11)

./gradlew :fabric:1.20.1:runClient
./gradlew :fabric:1.21.11:runClient
./gradlew :fabric:26.3:runClient

./gradlew :forge:1.20.1:runClient
./gradlew :neoforge:1.21.1:runClient
```

Сузить проекты (быстрее configure):

```bash
./gradlew -Pmod.loaders=fabric :fabric:1.21.11:runClient
./gradlew -Pmod.loaders=forge :forge:1.20.1:runClient
./gradlew -Pmod.loaders=neoforge :neoforge:1.21.1:runClient
```

## Активная версия Stonecutter

```bash
./gradlew "Set active project to 1.21.11"
./gradlew "Reset active project"   # → VCS 1.21.8
```

Сейчас по умолчанию: **1.21.11** (`stonecutter.gradle.kts`).

## Публикация

Только Fabric-ветки в одном прогоне (`-Pmod.loaders=fabric` подставьте при необходимости):

```bash
cp .env.example .env   # MODRINTH_* / CURSEFORGE_*
./gradlew -Pmod.loaders=fabric publishModrinth
./gradlew -Pmod.loaders=fabric publishCurseforge
```

`publishMods` отключён специально.

## Forge / NeoForge (заметки)

- Architectury Loom 1.9, `client` source set, `ClientHooks`
- Userdev: `syncMainResourcesToClient` (lang/textures); без второго `META-INF/mods.toml` в client
- `pack.mcmeta` лежит в `core/main/resources/`
- Remap JAR — только в изолированном прогоне (`buildAndCollectForge` / `NeoForge`)
- При `-Pmod.loaders=forge` / `neoforge` в settings поднимается **fabric stub** (`build-fabric-stub.gradle.kts`) без Loom — якорь Stonecutter active/vcs, чтобы FML-версия не стала active-деревом и не скомпилила сырой `core/`

## Полезное

```bash
./gradlew tasks --group=build
./gradlew :fabric:1.21.11:compileClientJava
./gradlew -Pmod.loaders=forge :forge:1.20.1:clientClasses
./gradlew clean
```
