# Changelog

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
- Multi-version build with [Stonecutter](https://stonecutter.kikugie.dev/): **1.21.8**, **1.21.10**, **1.21.11**
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

- Version-specific client overrides in `versions/1.21.8/src/client/java/` (legacy `mouseClicked` / `keyPressed` APIs)
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
