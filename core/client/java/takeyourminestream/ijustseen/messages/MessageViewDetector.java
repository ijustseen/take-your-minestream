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

        boolean modelVolume = ModConfig.isMESSAGE_3D_FRAME() && ModConfig.isSHOW_MESSAGE_BACKGROUND();
        return MessageClickHandler.findHit(
            client,
            message,
            tickCounter,
            ModConfig.getMAX_FREEZE_DISTANCE(),
            modelVolume
        ) != null;
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
}
