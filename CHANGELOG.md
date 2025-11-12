# Changelog

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
