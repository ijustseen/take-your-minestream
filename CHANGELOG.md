# Changelog

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
