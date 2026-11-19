package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;

/**
 * LOD мировых сообщений с гистерезисом.
 * <p>
 * Пороги только в блоках + конус взгляда — без углового размера:
 * слайдер размера сообщения не должен сдвигать дистанцию FULL.
 * HIDDEN — вне взгляда / слишком далеко.
 * SIMPLE — дешёвая плашка без текста/эмоутов.
 * FULL — полный рендер.
 */
public final class MessageRenderLod {
    public enum Level {
        HIDDEN,
        SIMPLE,
        FULL
    }

    /** Войти в FULL ближе этого. */
    private static final double FULL_ENTER = 30.0;
    /** Выйти из FULL дальше этого (гистерезис). */
    private static final double FULL_EXIT = 34.0;
    /** Soft-hide дальше. */
    private static final double CULL_ENTER = 96.0;
    /** Вернуться из HIDDEN по дистанции. */
    private static final double CULL_EXIT = 88.0;

    private static final double VIEW_ENTER = 0.22;
    private static final double VIEW_EXIT = 0.12;
    private static final double FULL_VIEW_ENTER = 0.48;
    private static final double FULL_VIEW_EXIT = 0.38;

    private MessageRenderLod() {}

    public static Level resolve(MinecraftClient client, Vec3d messagePos) {
        return resolve(client, messagePos, Level.HIDDEN);
    }

    public static Level resolve(
        MinecraftClient client,
        Vec3d messagePos,
        Level previous
    ) {
        if (client == null || client.player == null || messagePos == null) {
            return Level.HIDDEN;
        }

        Vec3d cameraPos = CameraPositionCompat.getCameraPos(client);
        double dist = cameraPos.distanceTo(messagePos);
        Level prev = previous != null ? previous : Level.HIDDEN;

        double cullHide = prev == Level.HIDDEN ? CULL_EXIT : CULL_ENTER;
        if (dist > cullHide) {
            return Level.HIDDEN;
        }
        if (dist < 1.0e-3) {
            return Level.FULL;
        }

        Vec3d look = client.player.getRotationVec(1.0f);
        Vec3d dir = messagePos.subtract(cameraPos).normalize();
        double dot = look.dotProduct(dir);

        double viewMin = prev == Level.HIDDEN ? VIEW_ENTER : VIEW_EXIT;
        if (dot < viewMin) {
            return Level.HIDDEN;
        }

        if (prev == Level.FULL) {
            if (dist <= FULL_EXIT && dot >= FULL_VIEW_EXIT) {
                return Level.FULL;
            }
            return Level.SIMPLE;
        }

        if (dist <= FULL_ENTER && dot >= FULL_VIEW_ENTER) {
            return Level.FULL;
        }
        return Level.SIMPLE;
    }

    /**
     * Совместимость со старыми вызовами: высота панели больше не влияет на LOD
     * (слайдер размера сообщения не должен менять дистанцию FULL).
     */
    public static Level resolve(
        MinecraftClient client,
        Vec3d messagePos,
        Level previous,
        float panelHeightPx
    ) {
        return resolve(client, messagePos, previous);
    }
}
