package takeyourminestream.ijustseen.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public final class CameraPositionCompat {
    private CameraPositionCompat() {
    }

    public static Vec3d getCameraPos(MinecraftClient client) {
        if (client == null) {
            return Vec3d.ZERO;
        }

        if (client.gameRenderer != null) {
            var camera = client.gameRenderer.getCamera();
            if (camera != null) {
                return camera.getCameraPos();
            }
        }

        if (client.player != null) {
            return client.player.getEyePos();
        }

        return Vec3d.ZERO;
    }
}
