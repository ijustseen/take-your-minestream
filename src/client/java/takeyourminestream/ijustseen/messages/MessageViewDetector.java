package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.config.ModConfig;

/**
 * Определяет, смотрит ли игрок на 3D-сообщение (заморозка при взгляде).
 */
public final class MessageViewDetector {
    private static final float MAX_FALL = 20.0f;

    private MessageViewDetector() {}

    public static boolean isPlayerLookingAtMessage(MinecraftClient client, Message message, int tickCounter) {
        if (client.player == null) {
            return false;
        }

        int effectiveAge = message.getEffectiveAge(tickCounter);
        if (effectiveAge >= ModConfig.getMESSAGE_LIFETIME_TICKS() + ModConfig.getMESSAGE_FALL_TICKS()) {
            return false;
        }

        Vec3d panelCenter = message.getPosition();

        Vec3d playerEyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        if (playerEyePos.distanceTo(panelCenter) > ModConfig.getMAX_FREEZE_DISTANCE()) {
            return false;
        }

        MessagePanelLayout.Dimensions dimensions = message.getWorldLayout(client.textRenderer);
        float finalScale = MessagePanelLayout.worldScale();
        double rectWidth = dimensions.panelWidth() * finalScale;
        double rectHeight = dimensions.panelHeight() * finalScale;
        float fallOffsetY = MessagePanelLayout.fallOffsetY(effectiveAge);

        return rayHitsMessageRectLocal(
            playerEyePos,
            lookVec,
            panelCenter,
            message.getYaw(),
            message.getPitch(),
            rectWidth,
            rectHeight,
            fallOffsetY * finalScale
        );
    }

    /**
     * Позиция центра панели с учётом падения (совпадает с {@code MessageRenderer}).
     */
    public static Vec3d calculateFallingPosition(Vec3d basePosition, int effectiveAge, float yaw, float pitch) {
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        int fallStart = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallAge = effectiveAge - fallStart;

        if (fallAge < 0) {
            return basePosition;
        }
        if (fallAge >= fallTicks) {
            return basePosition;
        }

        float fallProgress = (float) fallAge / (float) fallTicks;
        fallProgress = Math.min(Math.max(fallProgress, 0.0f), 1.0f);
        float fallOffsetYpx = (fallProgress * fallProgress) * MAX_FALL;
        float fallOffsetBlocks = fallOffsetYpx * MessagePanelLayout.worldScale();

        double pitchRad = Math.toRadians(pitch);
        Vec3d localYAxisWorld = new Vec3d(0.0, Math.cos(pitchRad), Math.sin(pitchRad));
        return basePosition.add(localYAxisWorld.multiply(-fallOffsetBlocks));
    }

    private static boolean rayHitsMessageRectLocal(
        Vec3d rayOrigin,
        Vec3d rayDir,
        Vec3d panelCenter,
        float panelYaw,
        float panelPitch,
        double rectWidth,
        double rectHeight,
        double localFallOffsetY
    ) {
        Vec3d origin = rayOrigin.subtract(panelCenter);
        Vec3d dir = rayDir;

        double yawRad = Math.toRadians(panelYaw);
        double pitchRad = Math.toRadians(panelPitch);
        origin = rotateX(origin, -pitchRad);
        origin = rotateY(origin, +yawRad);
        dir = rotateX(dir, -pitchRad);
        dir = rotateY(dir, +yawRad);

        double denom = dir.z;
        if (Math.abs(denom) < 1e-6) {
            return false;
        }
        double t = -origin.z / denom;
        if (t < 0) {
            return false;
        }

        Vec3d hitLocal = origin.add(dir.multiply(t));
        return Math.abs(hitLocal.x) <= rectWidth / 2.0
            && Math.abs(hitLocal.y - localFallOffsetY) <= rectHeight / 2.0;
    }

    private static Vec3d rotateY(Vec3d v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double x = v.x * cos + v.z * sin;
        double z = -v.x * sin + v.z * cos;
        return new Vec3d(x, v.y, z);
    }

    private static Vec3d rotateX(Vec3d v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double y = v.y * cos - v.z * sin;
        double z = v.y * sin + v.z * cos;
        return new Vec3d(v.x, y, z);
    }
}
