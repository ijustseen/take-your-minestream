package takeyourminestream.ijustseen.messages;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Кадровое сглаживание позиции и ориентации 3D-сообщений. */
public final class MessageRenderSmoothing {
    private static final double TAU_POS = 0.15;
    private static final double TAU_ANG = 0.12;

    private Vec3d pos;
    private float yaw;
    private float pitch;
    private long lastNs;

    public static MessageRenderSmoothing fromMessage(Message message) {
        MessageRenderSmoothing state = new MessageRenderSmoothing();
        state.pos = message.getPosition();
        state.yaw = message.getYaw();
        state.pitch = message.getPitch();
        state.lastNs = System.nanoTime();
        return state;
    }

    public void updateTowards(Message target) {
        long now = System.nanoTime();
        double dt = Math.max(0.0, (now - lastNs) / 1_000_000_000.0);
        lastNs = now;

        double alphaPos = 1.0 - Math.exp(-dt / TAU_POS);
        double alphaAng = 1.0 - Math.exp(-dt / TAU_ANG);

        Vec3d targetPos = target.getPosition();
        pos = new Vec3d(
            lerp(pos.x, targetPos.x, alphaPos),
            lerp(pos.y, targetPos.y, alphaPos),
            lerp(pos.z, targetPos.z, alphaPos)
        );
        yaw = lerpAngleDeg(yaw, target.getYaw(), (float) alphaAng);
        pitch = lerpAngleDeg(pitch, target.getPitch(), (float) alphaAng);
    }

    public Vec3d pos() {
        return pos;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static float lerpAngleDeg(float a, float b, float t) {
        float delta = MathHelper.wrapDegrees(b - a);
        return a + delta * t;
    }
}
