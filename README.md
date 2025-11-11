# Take Your MineStream

Advanced Twitch chat integration for Minecraft. Show chat messages in 3D or as a clean HUD overlay, with smart moderation and polished visuals.

## Features

- Multiple display modes: 3D world messages or HUD overlay
- Smart moderation: customizable banned words and auto‑censoring
- Smooth visuals: animations, particles, scalable message sizes
- Intelligent view detection: looking at a message pauses its lifetime
- Lightweight and performance‑friendly

## Compatibility

- Works with modern Minecraft versions supported by Fabric
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
- Keybinds: `]` open config; ~~`[` toggle Twitch connection~~

## Configuration highlights

- Display mode: 3D world or HUD overlay
- Message timing: lifetime, fall duration, spawn intervals
- Visuals: scale, colors, particle effects
- Moderation: banned words list and censor behavior

## Build from source

```bash
./gradlew build
```

## Development

For developers working on this mod:

- **[MINECRAFT_1.21.10_GUIDE.md](MINECRAFT_1.21.10_GUIDE.md)** - Complete guide for Minecraft 1.21.10 update
  - API changes and fixes
  - Custom rendering system
  - Usage examples
  - Troubleshooting

## License

MIT License. See `LICENSE` for details.
