# Changelog

## [2.0.0] - 2026-06-12

### MAJOR UPDATE — multi-platform chat, UI overhaul & rebrand

**Take Your MineStream** is now **Take Your Stream Chat** (`tysc`, mod id `take-your-stream-chat`). Settings and pins migrate automatically from 1.x (`take-your-minestream` folder / `tyms` jars).

This release is a breaking change in scope: chat is no longer Twitch-only, settings and HUD were reworked, and several gameplay defaults changed.

### Added

- **Multi-platform live chat**: Twitch, **YouTube**, **Kick**, and **TikTok** — connect several sources at once from Settings → General
- Unified chat stack: `ChatConnectionManager`, `ChatMessagePipeline`, `IncomingChatMessage`, per-platform clients
- **Platform icons** on message cards (3D, HUD, history) with accent-colored panel borders
- **Chat role filter** applies to **all platforms** (subscriber/member, VIP, mod, broadcaster) via shared badge parsing (`ChatAuthorRoles`)
- **HUD overlay** rework: slide-in animation, smooth fade-out, chat-like stacking (newest at bottom), nick color from platform when available
- HUD stays visible in **inventory and container screens**; hidden on ESC menu and other full screens
- **MessagePanelLayout** — single source of truth for 3D panel size (render, click hit-test, look-to-freeze)
- Localized UI and connection messages (en, ru, de, es, fr, zh — 162 keys each)
- Settings tab **icons** (General, Messages, In World, Chat History)
- `PlatformChannelRow`, `ConfigUiHelper`, `ChatConnectToggleHelper`; HTTP layer on `java.net.http.HttpClient` (Kick WAF compatibility)
- **Chat connect toggle** in settings footer (between Chat History and Done): ON/OFF label, colored dot, **green/red button background** by connection state; same action as the hotkey
- **Connection status overlay** (`ChatStatusNotifier`): connect/disconnect/connecting shown on the **action bar**, not in chat
- **TikTok custom emotes** in chat messages (inline emotes + subscriber emote stickers), loaded from TikTok CDN URLs
- **Spawn distance range** (min/max blocks) for Around and FOP modes in **In World** settings (default 2–5)
- **Color Unicode emojis** — optional setting (Settings → Messages) renders system emoji glyphs instead of Minecraft font squares; works in 3D, HUD, and history

### Changed

- **Settings screen**: platform toggles per service, channel fields; save reconnects changed platforms; footer **History · Chat · Done**
- **Chat notifications**: status on action bar; **errors only** in chat (offline / WAF / missing channel, etc.)
- **Chat History** (formerly Message History): opens scrolled to the bottom; orange history icon
- **In World** settings tab disabled when spawn mode is HUD
- Platform cannot be enabled with an empty channel/username
- **Message scale** presets reduced (~one step smaller): Tiny → 0.35, Normal → 0.75, Huge → 1.25 (Huge ≈ old Large)
- **3D interaction distance** capped at **16 blocks** (click, pin, drag)
- **Fall / fade duration** fixed at **0.5 s** — removed from settings (was user-configurable)
- **Spawn queue**: max **10** pending messages; oldest dropped on overflow; live messages show **immediately** (no artificial spacing)
- **YouTube poll batches**: when several messages arrive in one poll, they are sorted by `timestampUsec` and shown with real chat intervals (capped at 10 s gaps)
- Notification sound plays when a message **appears**, not when it enters the queue
- Role filter description updated for all platforms (all locales)
- Mod metadata description lists all supported platforms

- **Performance**: cached message panel/HUD layout and emote line wrapping (avoids per-frame recomputation); notification sound throttled to once per tick
- **3D break particles**: spawn at the **end of the fall** (shatter moment), positions aligned with `MessagePanelLayout` / renderer transforms
- **FOP spawn mode**: same random placement as **Around player** (radius, height), filtered to **horizontal FOV** with the player treated as looking at the horizon (yaw only, pitch ignored for the cone)

### Fixed

- **Click / hit detection** on large 3D messages with emotes and platform icon (was using wrong emote check and mismatched layout vs renderer)
- Fall animation offset synced between renderer and click/freeze detection
- **Kick** chat blocked by WAF (`ChatHttp` rewrite)
- **TikTok**: live room API (`sourceType=54`); protobuf **wire type 4** (END_GROUP) and unknown fields skipped without killing WSS; parse errors logged at debug, not as connection failures
- **YouTube**: offline channels no longer reported as “live chat unavailable for video ID” — detects non-live stream and shows **no live stream** for `@handle`; live detection handles spaced JSON, improved video ID selection and watch-page fallback
- **Platform toggles** — disabling chat or closing settings with platforms OFF no longer reconnects them on save/ESC
- **Color emoji setting** persisted correctly in config (`enableColorEmojis`); emoji textures load synchronously, no MC-font fallback for emoji provider
- **Russian** lang file JSON syntax (trailing comma) and key binding label
- **Mixin** client config package fixed after rebrand (`takeyourminestream.ijustseen.mixin.client`)
- **Spawn chance** slider: 0% no longer spawns ~1% of messages (`>= chance` fix)
- **NORMAL** message scale was incorrectly 0.8 instead of intended baseline
- YouTube initial chat backlog still skipped; live poll batches spread by message timestamps when multiple arrive at once

### Removed

- Twitch-only IRC toggle helper (`TwitchToggleHelper`)
- **Message fall (seconds)** setting — fixed internal timing
- HUD left-click to dismiss (not viable with in-game cursor)
- Queue clear on full disconnect (short capped queue makes tail acceptable)

### Technical

- `MessageHudOverlay`, `MessageHudVisibility`; Minecraft **26.1** HUD via `HudElementRegistry`
- Version-specific `MessageHudRenderer` / `MouseHandlerMixin` where matrix and mouse APIs differ
- Stonecutter replacements extended (GUI scaled size, `HandledScreen` → `AbstractContainerScreen` on 26.1)
- Primary client command: `/streamchat` (`/minestream` kept as alias)
- Lang namespace: `takeyourstreamchat.*`
- `ChatStatusNotifier`, `ChatErrorReporter` (overlay vs chat routing); `client.inGameHud` → `client.gui` on 26.1

## [1.4.3] - 2026-06-02

### Changed

- All console log messages are now in English
- Removed verbose debug logging (IRC chat echo, emote load/register traces, 7TV success counters, settings-screen info)

### Fixed

- Crash when rendering 3D message panels on Minecraft 1.21.11 with Iris/Sodium (and related modded renderers): `RenderLayerCompat` now calls `RenderLayers` / `RenderTypes` directly instead of reflection (also fixes the incomplete 1.4.2 fix that could still throw `Unable to resolve entity RenderLayer`)
- `CameraPositionCompat` uses direct camera position API per Minecraft version instead of reflection
- History action popup: card text no longer draws on top of the menu — on Minecraft 1.21–1.21.4 popup fills use `RenderLayer.getGuiOverlay()`; on 1.21.8+ a new GUI root layer is created before the popup

## [1.4.2] - 2026-06-02

### Added

- **Unpin mode** setting (Settings → Behavior): choose how to unpin 3D messages with right-click
  - **Pin icon only** — unpin only when clicking the pin icon; click elsewhere on the message to drag
  - **Whole message** — quick right-click anywhere on the message unpins it; holding right-click drags

### Fixed

- Crash when rendering 3D message panels on Minecraft 1.21.11 (NPE in `VertexConsumerProvider.getBuffer` when `RenderLayerCompat` returned null)
- Broken 9-slice panels in history/pinned columns on Minecraft 1.21 / 1.21.1 (legacy `drawTexture` ignored texture region size)
- Emotes not rendered in message history and pinned columns (only plain text was drawn)
- Unpin mode «Whole message»: a quick right-click on an unpinned message no longer pins and immediately unpins it

## [1.4.1] - 2026-06-02

### Added

- **Message history screen** with context menu: pin/unpin, block user, replay message
- **Blocked username list** and block action from history
- **Chat role filter** (Twitch badges: broadcaster, mod, VIP, etc.) in config
- **Two-column history UI**: full history (left) + all pinned messages in the world (right), centered layout
- **Message history actions** (`MessageHistoryActions`): pin copies with `historySourceId`, replay, block
- Reusable GUI layer: `MessageCardLayout`, `MessageCardRenderer`, `MessagePanelGuiRenderer`, `GuiScrollbar`, `HistoryMessageActionPopup`, `ScreenUiHelper`, `ModUiTheme`
- **9-slice panels** for world and HUD message rendering (`MessagePanel9Slice`, `MessagePanelWorldRenderer`)
- **HUD emote rendering** (`MessageEmoteGuiRenderer`); pinned messages hidden from HUD overlay
- Configurable **message history size** (10–500)
- **Chance-to-spawn** slider and numeric fields for lifetime, fall duration, max freeze distance
- Multi-version build with [Stonecutter](https://stonecutter.kikugie.dev/): **1.21**, **1.21.1**, **1.21.4**, **1.21.8**, **1.21.10**, **1.21.11**, **26.1** (covers **26.1.1** and **26.1.2** via one JAR)
- Gradle tasks `buildAndCollect`, `buildActive`, `runActive`; [BUILD.md](BUILD.md) reference
- Package refactor: `config/`, `core/`, `integration/twitch/`, `ui/screen/`, `ui/gui/`, `filtering/`

### Changed

- **Settings screen** redesigned: tabs, scrollable list, fixed footer with description and buttons; category labels
- **Banword / regexp / blocked-username** list screens aligned with new UI theme
- Message history: compact panels, independent scroll per column, live refresh on pin/unpin and history cap
- HUD overlay: text wrap width aligned with 3D mode (120 px)
- **Pinned message persistence** (`PinnedMessageStore`): improved world save/load and pin dimensions
- **World join/leave** handling updates for pinned messages and IRC state
- Build migrated from Groovy `build.gradle` to Kotlin `build.gradle.kts` + `settings.gradle.kts`
- README and Modrinth README updated; translations (en, ru, de, es, fr, zh)

### Fixed

- Message history content clipped on the left (card X vs scissor padding mismatch)
- Scrollbar track and thumb extending below/above the list panel
- Message history not updating when history limit was reached (same size, different messages)
- Button hover in custom-rendered screens
- Pin icon and panel hitbox dimensions
- `RenderLayerCompat` for Minecraft 1.21.11
- Translucent rendering for removed/gone messages in history
- Unpin from pinned column correctly removes history-linked pin copies

### Technical

- Version-specific client overrides in `versions/1.21.8/src/client/java/` (legacy `mouseClicked` / `keyPressed` APIs), reused for **1.21–1.21.4** with extra legacy GUI/HUD render paths
- **Minecraft 26.1** port: Mojang mappings, `build-unobfuscated.gradle.kts`, Stonecutter replacements, overrides in `versions/26.1/src/` (`MouseHandlerMixin`, HUD, key bindings, sliders, `MessagePosition`, and related UI)
- `PlayerMessageCompat` for chat feedback across 1.21.x and 26.1
- Per-version dependencies in `versions/<mc>/gradle.properties`
- `MessageLifecycleManager.getAllPinnedMessages()` — all active pinned messages, not only those still in history

## [1.4.0] - 2026-03-13

### MAJOR UPDATE

- Emotes rendering added and stabilized:
  - Twitch emotes from IRC tags
  - 7TV emotes (global + channel)
  - Animated 7TV GIF rendering with frame timing
  - Local disk cache for Twitch and 7TV emotes
- Config storage moved from `config/` to top-level `take-your-minestream/` folder
  - Main JSON config moved inside this folder
  - Legacy data migration added
- Message interaction hitboxes fixed to match visual panel/pin geometry under rotations
- Message interactions (remove/pin/unpin/drag) now work only with empty main hand
- Auto IRC connect on world/server join added (config toggle)

## [1.3.0] - 2026-03-07

- Now you can pin messages
- Massages behavior fixes with
- Pinned messages can be copied from world in config folder
- IRC fix

## [1.2.0] - 2024-11-12

### Added

- Click-to-remove message functionality with particle effects
- Configuration option to enable/disable click-to-remove feature
- World event handling system for message parsing pause/resume
- Automatic message parsing pause when leaving worlds
- Automatic message parsing resume when joining worlds

### Changed

- Updated Minecraft version from 1.21.7 to 1.21.10
- Updated Fabric API to 0.129.0+1.21.7
- Fixed WorldRenderEvents imports (moved to v1.world package)
- Updated rendering API calls (matrixStack() → matrices())
- Fixed MinecraftClient.disconnect() mixin signature for new boolean parameter

### Technical

- MouseHandlerMixin for click detection on messages
- MessageClickHandler for click processing and particle spawning
- WorldEventHandler for managing message parsing state
- ClientPlayNetworkHandlerMixin for world join detection
- MinecraftClientMixin for world leave detection

## [1.1.0] - 2024-11-11

### Added

- Custom render pipeline system (CustomRenderPipeline.java, WaypointRenderer.java)
- GPU resource cleanup (GameRendererMixin.java)
- Rendering guide (RENDERING_GUIDE.md)

### Technical

- Support for extraction/drawing phase rendering
- Example waypoint renderer with through-wall rendering
- Proper GPU memory management
