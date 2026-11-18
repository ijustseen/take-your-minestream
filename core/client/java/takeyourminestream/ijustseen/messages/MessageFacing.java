package takeyourminestream.ijustseen.messages;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Ориентация панели «лицом к глазам».
 * При почти вертикальном направлении XZ→0 и {@code atan2} даёт случайный yaw (часто «на запад») —
 * тогда оставляем прежний yaw, чтобы не было резкого переворота.
 */
public final class MessageFacing {
    /** Ниже этого горизонтального пролёта считаем направление вертикальным. */
    private static final double MIN_HORIZONTAL = 1.0e-3;

    private MessageFacing() {}

    public record Angles(float yaw, float pitch) {}

    /**
     * Углы, при которых панель в {@code target} смотрит на {@code eye}.
     * {@code fallbackYaw} — текущий yaw сообщения (или игрока), если цель прямо сверху/снизу.
     */
    public static Angles towardEye(Vec3d eye, Vec3d target, float fallbackYaw) {
        double dx = eye.x - target.x;
        double dy = eye.y - target.y;
        double dz = eye.z - target.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = distXZ < MIN_HORIZONTAL
            ? fallbackYaw
            : (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(MathHelper.atan2(dy, distXZ) * (180.0 / Math.PI));
        return new Angles(yaw, pitch);
    }
}
