package takeyourminestream.ijustseen.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;

/** Перспектива камеры и поворот игрока для предпросмотра в редакторе сообщений. */
public final class EditorCameraCompat {
    private EditorCameraCompat() {}

    public static Object capturePerspective(MinecraftClient client) {
        return client.options.getPerspective();
    }

    public static void restorePerspective(MinecraftClient client, Object perspective) {
        if (perspective instanceof Perspective value) {
            client.options.setPerspective(value);
        }
    }

    public static void setThirdPersonBack(MinecraftClient client) {
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static float yaw(Entity entity) {
        return entity.getYaw();
    }

    public static float pitch(Entity entity) {
        return entity.getPitch();
    }

    /** Ставит поворот сразу, без межтиковой интерполяции (игра на паузе, пока открыт редактор). */
    public static void setRotation(Entity entity, float yaw, float pitch) {
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        entity.lastYaw = yaw;
        entity.lastPitch = pitch;
        entity.setHeadYaw(yaw);
    }
}
