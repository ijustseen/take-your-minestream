# Take Your Stream Chat

Live chat from **Twitch, YouTube, Kick & TikTok** in Minecraft — as 3D billboards or a HUD overlay. For streamers and players who want chat in the game, not in a second monitor.

## Features

- Multi-platform chat (enable any combination in settings)
- 3D messages in the world or HUD overlay (visible in inventory)
- Twitch & 7TV emotes, platform-colored borders
- Pin messages, chat history, replay, username blocklist
- Banwords, regex filters, role filter (subs / VIP / mods)
- Lightweight client-side mod (Fabric; Forge / NeoForge on selected versions)

## Compatibility

| Loader | Minecraft |
|--------|-----------|
| **Fabric** | 1.20.1, 1.21, 1.21.1, 1.21.4, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.2, 26.3 |
| **Forge** | 1.20.1 |
| **NeoForge** | 1.21.1 |

One jar per game version (and loader). The 1.20.1 jars need Java 17.

**Fabric:** Loader ≥0.16.14 and Fabric API; Mod Menu optional.  
**Forge / NeoForge:** matching loader for that Minecraft version (no Fabric API).

## Installation

1. Install **Fabric**, **Forge**, or **NeoForge** for your Minecraft version
2. Download the matching jar (e.g. `tysc-2.2.2+1.21.11.jar`, `tysc-2.2.2+1.20.1-forge.jar`)
3. Place in `mods/` and launch
4. Press **`]`** → enable platforms, enter channels, save

## Commands

- `/streamchat test <text>` — test message
- `/streamchat chat start` | `chat stop` — connect / disconnect
- `/streamchat banword` / `blockuser` — moderation
- `/minestream` — legacy alias (same commands)

Keybinds: **`]`** settings, **`[`** quick chat toggle, **`\`** chat history

## Build from source

See [BUILD.md](BUILD.md). Shared code lives in `core/`; each loader is built separately:

```bash
./gradlew buildAndCollectFabric
./gradlew buildAndCollectForge
./gradlew buildAndCollectNeoForge
# or all:
./gradlew buildAndCollectAll
```

## License

MIT — see [LICENSE](LICENSE).

## Links

- [Modrinth](https://modrinth.com/mod/take-your-stream-chat) (project page)
- [GitHub](https://github.com/ijustseen/take-your-minestream)
