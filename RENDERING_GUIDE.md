# Rendering Guide (current branch: 1.21.11)

This document describes the rendering flow that is actually used in the current codebase.

## Scope

- 3D world rendering for chat messages
- HUD rendering path
- Emote rendering (Twitch + 7TV via internal cache/providers)
- Click hit detection for message interaction

## Main entry points

- `MessageRenderer` renders world messages and panel/background visuals.
- `MessageHudRenderer` renders HUD mode.
- `MessageClickHandler` resolves hit detection for clicks and pin interactions.
- `TwitchEmoteTextureCache` handles emote texture loading/caching.
- `SevenTVEmoteProvider` resolves 7TV emotes by code.

## Render flow

1. New messages are queued by `MessageQueue`.
2. `MessageSpawner` creates `Message` entities with position, orientation, and optional emote metadata.
3. `MessageLifecycleManager` updates age, freeze logic, and cleanup.
4. `MessageRenderer` draws world-space text panels and emotes each frame.
5. Optional pin/click logic is applied through `MessageClickHandler` and `PinnedMessageInteractionManager`.

## Emote rendering flow

1. Twitch IRC tags are parsed in `TwitchChatClient`.
2. Twitch emotes are converted into `MessageEmote` ranges.
3. 7TV scan augments the same line with extra emote ranges.
4. Texture preloading is triggered via `TwitchEmoteTextureCache.preload(...)`.
5. `MessageRenderer` draws either glyph text segments or textured emote quads.

## Important implementation notes

- No external Streamotes dependency is required in this branch.
- The old experimental waypoint/custom pipeline files are not part of the active render path.
- Message panel/background uses the internal texture-based panel rendering.
- View-freeze and click hitboxes are computed against actual message plane/size.

## Compatibility

- Minecraft: 1.21.11
- Fabric Loader: 0.17.3+
- Fabric API: 0.137.0+
- Java: 21

## Debug checklist

- Emotes missing: verify provider parsing and texture preload path.
- Click misses: verify message orientation and hitbox math in `MessageClickHandler`.
- Disappearing messages: verify lifetime/freeze settings in `ModConfig`.
