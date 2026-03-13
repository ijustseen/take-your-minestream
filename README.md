# Take Your MineStream

Advanced Twitch chat integration for Minecraft. Show chat messages in 3D or as a clean HUD overlay, with smart moderation and polished visuals.

## Features

- Multiple display modes: 3D world messages or HUD overlay
- Smart moderation: customizable banned words and auto‑censoring
- Smooth visuals: animations, particles, scalable message sizes
- Intelligent view detection: looking at a message pauses its lifetime
- Lightweight and performance‑friendly

## Compatibility

- Target branch compatibility: Minecraft 1.21.7 / 1.21.8 (Fabric)
- Requires Fabric Loader and Fabric API
- See releases for exact version support

## Installation

1. Install Fabric Loader (see supported versions above)
2. Download the mod `.jar`
3. Put the `.jar` into your `mods` folder
4. Launch the game

## Quick start

- Open the mod settings in‑game (default key: `]`)
- Set your Twitch channel name
- Connect to Twitch and start streaming

## Commands & keybinds

- `/minestream test <message>` — show a test message
- `/minestream twitch start` | `/minestream twitch stop` — control Twitch connection
- `/minestream banword add|remove|list` — manage content filter
- `/minestream help` — list all commands
- Keybinds: `]` open config, `[` quick IRC start/stop toggle

## Configuration highlights

- Display mode: 3D world or HUD overlay
- Message timing: lifetime, fall duration, spawn intervals
- Visuals: scale, colors, particle effects
- Moderation: banned words list and censor behavior
- Interaction note: message click/pin interactions work only with an empty main hand

## Build from source

```bash
./gradlew build
```

## Development

For developers working on this mod:

- **[RENDERING_GUIDE.md](RENDERING_GUIDE.md)** - Current rendering and emote flow
- **[src/client/java/takeyourminestream/ijustseen/messages/README.md](src/client/java/takeyourminestream/ijustseen/messages/README.md)** - Message system architecture details

## License

MIT License. See `LICENSE` for details.
