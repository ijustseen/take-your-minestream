# Take Your MineStream

Bring your Twitch chat directly into Minecraft.

Turn viewer messages into in-game moments with clean visuals, smart moderation, and streamer-friendly controls.

## ✨ Highlights

- 💬 **Live chat in Minecraft** — show messages in 3D space or HUD mode
- 🧠 **Smart moderation** — banwords, regex filters, and Twitch badge role filter
- 🎭 **Emote support** — Twitch and 7TV emotes in message rendering
- 📌 **Pinned messages** — keep important chat visible in-world (saved per world/server)
- 📜 **Message history** — browse chat, pin/unpin, replay, or block users from a two-column screen
- ⚙️ **In-game setup** — configure channel and behavior without leaving Minecraft
- 🚀 **Lightweight** — built for smooth gameplay while streaming

## 🕹️ What you can do

- Display chat as floating world messages or as a HUD overlay
- Pause message lifetime while looking at messages (easy reading during action)
- Toggle Twitch connection in-game
- Tune timing, scale, visuals, and spawn behavior
- Click to remove messages; pin, drag, and unpin in-world
- Open message history: pin again, replay, or block a username
- Interaction safety: click/pin actions work only with an empty main hand

## 🚀 Quick start

1. Install **Fabric Loader** and **Fabric API** for your Minecraft version
2. Download the matching release jar (see **Compatibility** below)
3. Put the `.jar` into your `mods` folder
4. Launch the game and press `]` to open settings
5. Enter your Twitch channel name and connect

> **Mod Menu** is optional — if installed, the mod also appears in the mod list. Settings always work via `]`.

## ⌨️ Commands

- `/minestream test <message>` — show a test message
- `/minestream twitch start` — connect to Twitch
- `/minestream twitch stop` — disconnect from Twitch
- `/minestream banword add|remove|list` — manage filter words
- `/minestream blockuser add|remove|list` — manage blocked usernames
- `/minestream help` — list all commands

## 🔧 Compatibility

Pick the jar that matches your game version:

| Minecraft | Release file | Java |
|-----------|--------------|------|
| **1.21.8** | `tyms-1.4.1+1.21.8.jar` | 21 |
| **1.21.10** | `tyms-1.4.1+1.21.10.jar` | 21 |
| **1.21.11** | `tyms-1.4.1+1.21.11.jar` | 21 |
| **26.1** / **26.1.1** / **26.1.2** | `tyms-1.4.1+26.1.jar` | 25 |

**Required:** Fabric Loader + Fabric API (no other mods required).

**Optional:** Mod Menu.

## 🎮 Keybinds

- `]` — open mod settings
- `[` — quick IRC start/stop toggle

## 🛠️ Customize your stream look

- Message mode: world / HUD
- Message timing and fall behavior
- Scaling, sound, and particle settings
- Moderation: banwords, regex rules, blocked users, Twitch role filter
- Message history size and pinned-message persistence

---

**Make chat part of the gameplay.**

For support, issues, and contributions: [GitHub](https://github.com/ijustseen/take-your-minestream)
