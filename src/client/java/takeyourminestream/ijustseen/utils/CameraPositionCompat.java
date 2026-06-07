package takeyourminestream.ijustseen.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CameraPositionCompat {
    private static final Map<Class<?>, Method> CAMERA_POS_METHOD_CACHE = new ConcurrentHashMap<>();

    private CameraPositionCompat() {
    }

    public static Vec3d getCameraPos(MinecraftClient client) {
        if (client == null) {
            return Vec3d.ZERO;
        }

        Object camera = null;
        if (client.gameRenderer != null) {
            camera = client.gameRenderer.getCamera();
        }

        if (camera != null) {
            Vec3d reflectedPosition = invokeCameraPos(camera);
            if (reflectedPosition != null) {
                return reflectedPosition;
            }
        }

        if (client.player != null) {
            return client.player.getEyePos();
        }

        return Vec3d.ZERO;
    }

    private static Vec3d invokeCameraPos(Object camera) {
        Class<?> cameraClass = camera.getClass();
        Method method = CAMERA_POS_METHOD_CACHE.get(cameraClass);
        if (method == null) {
            Method resolved = resolveCameraPosMethod(cameraClass);
            if (resolved != null) {
                CAMERA_POS_METHOD_CACHE.putIfAbsent(cameraClass, resolved);
                method = resolved;
            }
        }

        if (method == null) {
            return null;
        }

        try {
            Object value = method.invoke(camera);
            if (value instanceof Vec3d vec3d) {
                return vec3d;
            }
        } catch (ReflectiveOperationException ignored) {
            CAMERA_POS_METHOD_CACHE.remove(cameraClass);
        }

        return null;
    }

    private static Method resolveCameraPosMethod(Class<?> cameraClass) {
        Method preferred = null;

        for (Method method : cameraClass.getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != Vec3d.class) {
                continue;
            }

            if ("getPos".equals(method.getName())) {
                return method;
            }

            if (preferred == null || method.getName().startsWith("method_")) {
                preferred = method;
            }
        }

        return preferred;
    }
}
