package takeyourminestream.ijustseen.utils;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Перспектива камеры и поворот игрока для предпросмотра в редакторе сообщений. */
public final class EditorCameraCompat {
    private EditorCameraCompat() {}

    public static Object capturePerspective(Minecraft client) {
        return client.options.getCameraType();
    }

    public static void restorePerspective(Minecraft client, Object perspective) {
        if (perspective instanceof CameraType value) {
            client.options.setCameraType(value);
        }
    }

    public static void setThirdPersonBack(Minecraft client) {
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public static float yaw(Entity entity) {
        return entity.getYRot();
    }

    public static float pitch(Entity entity) {
        return entity.getXRot();
    }

    /** Ставит поворот сразу, без межтиковой интерполяции (игра на паузе, пока открыт редактор). */
    public static void setRotation(Entity entity, float yaw, float pitch) {
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.setYHeadRot(yaw);
    }
}
