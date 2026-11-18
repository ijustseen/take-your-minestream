# Developing

Общий код и ассеты — в **`core/`**. Лоадеры (`fabric` / `forge` / `neoforge`) подключают его симлинком `src → ../core` и добавляют entrypoint + MC-overrides в `versions/{mc}/src/`.

Подробности сборки: **[BUILD.md](BUILD.md)**.

## Команды

```bash
# JAR по лоадерам (изолированно — так нужно из‑за Loom)
./gradlew buildAndCollectFabric
./gradlew buildAndCollectForge
./gradlew buildAndCollectNeoForge
./gradlew buildAndCollectAll

# Клиент
./gradlew :fabric:1.21.11:runClient
./gradlew :forge:1.20.1:runClient
./gradlew :neoforge:1.21.1:runClient
```

## Куда класть код

| Что | Куда |
|-----|------|
| Чаты, фильтры, UI, рендер, ассеты | `core/` |
| Fabric / Forge / NeoForge hooks | `core/.../platform/` + entrypoint |
| Ломающий API конкретной MC | `{loader}/versions/{mc}/src/…` |
| Зависимости версии | `{loader}/versions/{mc}/gradle.properties` |

## Версия релиза

1. `mod.version` в `gradle.properties`
2. Секция в `CHANGELOG.md`
3. `./gradlew buildAndCollectAll` (или нужный лоадер)
