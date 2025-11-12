# Changelog

## [1.2.0] - 2024-11-12

### Added

- Click-to-remove message functionality with particle effects
- Configuration option to enable/disable click-to-remove feature
- World event handling system for message parsing pause/resume
- Automatic message parsing pause when leaving worlds
- Automatic message parsing resume when joining worlds

### Changed

- Fixed MinecraftClient.disconnect() mixin signature for new boolean parameter

### Technical

- MouseHandlerMixin for click detection on messages
- MessageClickHandler for click processing and particle spawning
- WorldEventHandler for managing message parsing state
- ClientPlayNetworkHandlerMixin for world join detection
- MinecraftClientMixin for world leave detection
