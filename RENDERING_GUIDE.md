# Custom Rendering Guide for Minecraft 1.21.7

## Quick Start

### Test the example waypoint

```bash
./gradlew runClient
# In game: /tp 0 100 0
```

You should see a green cube that renders through walls.

## What was added

### New files

1. **CustomRenderPipeline.java** - Basic example from Fabric docs

   - Renders a green cube at (0, 100, 0)
   - Renders through walls (NO_DEPTH_TEST)
   - Demonstrates extraction/drawing phases

2. **WaypointRenderer.java** - Full-featured renderer

   - Support for multiple waypoints
   - Different colors and sizes
   - Ready to use in your project

3. **GameRendererMixin.java** - Resource cleanup
   - Prevents GPU memory leaks
   - Called when GameRenderer closes

## Usage Examples

### Basic - Single waypoint

```java
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import takeyourminestream.modid.rendering.WaypointRenderer;
import takeyourminestream.modid.rendering.WaypointRenderer.Waypoint;

WaypointRenderer renderer = new WaypointRenderer();

// Add a green waypoint
renderer.addWaypoint(Waypoint.green(new Vec3d(0, 100, 0)));

// Register rendering
WorldRenderEvents.AFTER_ENTITIES.register(context -> {
    renderer.render(context);
});
```

### Multiple waypoints

```java
renderer.addWaypoint(Waypoint.green(new Vec3d(0, 100, 0)));
renderer.addWaypoint(Waypoint.red(new Vec3d(10, 100, 10)));
renderer.addWaypoint(Waypoint.blue(new Vec3d(-10, 100, -10)));

// Custom waypoint
renderer.addWaypoint(new Waypoint(
    new Vec3d(20, 100, 20),  // position
    1f, 1f, 0f,              // yellow (RGB)
    0.7f,                    // alpha
    2f                       // size in blocks
));
```

### Dynamic waypoints

```java
WorldRenderEvents.AFTER_ENTITIES.register(context -> {
    renderer.clearWaypoints();

    // Add waypoints for active messages
    for (Message msg : activeMessages) {
        renderer.addWaypoint(Waypoint.green(msg.getPosition()));
    }

    renderer.render(context);
});
```

## Disable test waypoint

If you don't want the test waypoint, edit `fabric.mod.json`:

```json
"client": [
  "takeyourminestream.modid.TakeYourMineStreamClient"
  // "takeyourminestream.modid.rendering.CustomRenderPipeline"
]
```

## How it works

### Two-phase rendering

1. **Extraction** - Collect data for rendering
   - Call `VertexRendering.drawFilledBox()`
   - Write vertices to BufferBuilder
2. **Drawing** - Render to screen
   - Build the buffer
   - Upload to GPU
   - Execute draw call

This allows rendering the previous frame in parallel with extracting the next frame.

### Custom pipeline

```java
private static final RenderPipeline MY_PIPELINE = RenderPipelines.register(
    RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation(Identifier.of("mod-id", "pipeline/my_pipeline"))
        .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_STRIP)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()
);
```

### Resource cleanup

**IMPORTANT:** Always clean up resources!

```java
public void close() {
    allocator.close();
    if (vertexBuffer != null) {
        vertexBuffer.close();
        vertexBuffer = null;
    }
}
```

The GameRendererMixin calls this automatically.

## Performance tips

1. Reuse BufferBuilder (don't create new each frame)
2. Use MappableRingBuffer for automatic buffer management
3. Group objects by pipeline
4. Clear unused waypoints

## Compatibility

- ✅ Minecraft 1.21.7
- ✅ Fabric Loader 0.16.14+
- ✅ Fabric API 0.129.0+
- ✅ Java 21

## Links

- [Fabric Rendering Documentation](https://docs.fabricmc.net/develop/rendering/world)
- [Rendering Concepts](https://docs.fabricmc.net/develop/rendering/basic-concepts)
