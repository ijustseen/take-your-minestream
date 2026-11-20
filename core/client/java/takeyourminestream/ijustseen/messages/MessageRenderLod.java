package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;
import takeyourminestream.ijustseen.utils.EditorCameraCompat;

/**
 * LOD мировых сообщений с гистерезисом.
 * <p>
 * Пороги дистанции в блоках (слайдер размера сообщения не должен сдвигать дистанцию FULL)
 * + конус видимости по реальному FOV и соотношению сторон окна.
 * HIDDEN — за пределами экрана / слишком далеко.
 * SIMPLE — дешёвая плашка без текста/эмоутов (только по дистанции).
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

    /** Запас на динамический FOV (спринт, зелья скорости) поверх значения из настроек. */
    private static final double DYNAMIC_FOV_HEADROOM = 1.2;
    /** Полуразмер панели в блоках: край крупной панели виден, даже когда центр уже за экраном. */
    private static final double PANEL_RADIUS_BLOCKS = 2.5;
    /** Гистерезис конуса видимости, градусы. */
    private static final double VIEW_EXIT_EXTRA_DEG = 6.0;
    private static final double MAX_VIEW_HALF_ANGLE_DEG = 175.0;

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

        Vec3d dir = messagePos.subtract(cameraPos).normalize();
        double dot = cameraLook(client).dotProduct(dir);

        double halfAngle = screenHalfAngleDeg(client) + Math.toDegrees(Math.atan(PANEL_RADIUS_BLOCKS / dist));
        if (prev != Level.HIDDEN) {
            halfAngle += VIEW_EXIT_EXTRA_DEG;
        }
        halfAngle = Math.min(halfAngle, MAX_VIEW_HALF_ANGLE_DEG);
        if (dot < Math.cos(Math.toRadians(halfAngle))) {
            return Level.HIDDEN;
        }

        double fullMax = prev == Level.FULL ? FULL_EXIT : FULL_ENTER;
        return dist <= fullMax ? Level.FULL : Level.SIMPLE;
    }

    /**
     * Направление камеры: взгляд игрока, развёрнутый в виде от 3-го лица спереди.
     * Позицию игрока не используем — она не интерполирована между тиками и при движении
     * расходится с камерой, из-за чего сообщения мерцали.
     */
    private static Vec3d cameraLook(MinecraftClient client) {
        Vec3d look = client.player.getRotationVec(1.0f);
        Object perspective = EditorCameraCompat.capturePerspective(client);
        if (perspective instanceof Enum<?> mode && "THIRD_PERSON_FRONT".equals(mode.name())) {
            return new Vec3d(-look.x, -look.y, -look.z);
        }
        return look;
    }

    /** Угол от центра до угла экрана с учётом FOV и соотношения сторон окна. */
    private static double screenHalfAngleDeg(MinecraftClient client) {
        double verticalFov = client.options.getFov().getValue() * DYNAMIC_FOV_HEADROOM;
        double tanV = Math.tan(Math.toRadians(Math.min(verticalFov, 170.0) / 2.0));
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        double aspect = width > 0 && height > 0 ? (double) width / height : 16.0 / 9.0;
        double tanH = tanV * aspect;
        return Math.toDegrees(Math.atan(Math.sqrt(tanV * tanV + tanH * tanH)));
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
