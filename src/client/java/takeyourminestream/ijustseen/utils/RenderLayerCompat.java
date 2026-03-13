package takeyourminestream.ijustseen.utils;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public final class RenderLayerCompat {
    private static final List<String> PREFERRED_METHOD_NAMES = Arrays.asList(
        "getEntityTranslucent",
        "method_23580",
        "getText",
        "getEntityCutoutNoCull",
        "getEntityCutoutNoCullZOffset",
        "getEntitySolid"
    );

    private static volatile Method cachedEntityTextureMethod;

    private RenderLayerCompat() {
    }

    public static RenderLayer getEntityTextureLayer(Identifier texture) {
        Method method = cachedEntityTextureMethod;
        if (method == null) {
            method = resolveEntityTextureMethod();
            cachedEntityTextureMethod = method;
        }

        if (method != null) {
            try {
                Object result = method.invoke(null, texture);
                if (result instanceof RenderLayer renderLayer) {
                    return renderLayer;
                }
            } catch (ReflectiveOperationException ignored) {
                cachedEntityTextureMethod = null;
            }
        }

        return RenderLayer.getEntitySolid(texture);
    }

    private static Method resolveEntityTextureMethod() {
        Method[] methods = RenderLayer.class.getMethods();

        for (String preferredName : PREFERRED_METHOD_NAMES) {
            for (Method method : methods) {
                if (isMatchingEntityTextureMethod(method) && preferredName.equals(method.getName())) {
                    return method;
                }
            }
        }

        for (Method method : methods) {
            if (isMatchingEntityTextureMethod(method)) {
                return method;
            }
        }

        return null;
    }

    private static boolean isMatchingEntityTextureMethod(Method method) {
        return Modifier.isStatic(method.getModifiers())
            && method.getReturnType() == RenderLayer.class
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == Identifier.class;
    }
}