package takeyourminestream.ijustseen.utils;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public final class RenderLayerCompat {
    private static final List<String> NEW_ENTITY_METHOD_NAMES = Arrays.asList(
        "entityTranslucent",
        "entityCutoutNoCull",
        "entityCutoutNoCullZOffset",
        "entitySolid"
    );

    private static final List<String> LEGACY_ENTITY_METHOD_NAMES = Arrays.asList(
        "getEntityTranslucent",
        "method_23580",
        "getEntityCutoutNoCull",
        "getEntityCutoutNoCullZOffset",
        "getEntitySolid"
    );

    private static final List<String> NEW_TEXT_METHOD_NAMES = List.of("text");
    private static final List<String> LEGACY_TEXT_METHOD_NAMES = Arrays.asList(
        "getText",
        "getGuiTextured",
        "method_23582"
    );

    private static volatile Method cachedEntityTextureMethod;
    private static volatile Method cachedTextLayerMethod;
    private static volatile Class<?> cachedLayerHostClass;

    private RenderLayerCompat() {
    }

    public static RenderLayer getEntityTextureLayer(Identifier texture) {
        return invokeLayerMethod(cachedEntityTextureMethod, texture, true, true);
    }

    public static RenderLayer getTextLayer(Identifier texture) {
        RenderLayer textLayer = invokeLayerMethod(cachedTextLayerMethod, texture, false, false);
        if (textLayer != null) {
            return textLayer;
        }
        return getEntityTextureLayer(texture);
    }

    private static RenderLayer invokeLayerMethod(
        Method cached,
        Identifier texture,
        boolean cacheEntityField,
        boolean entityMethods
    ) {
        Method method = cached;
        if (method == null) {
            List<String> newNames = entityMethods ? NEW_ENTITY_METHOD_NAMES : NEW_TEXT_METHOD_NAMES;
            List<String> legacyNames = entityMethods ? LEGACY_ENTITY_METHOD_NAMES : LEGACY_TEXT_METHOD_NAMES;
            method = resolveLayerMethod(newNames, legacyNames);
            if (cacheEntityField) {
                cachedEntityTextureMethod = method;
            } else {
                cachedTextLayerMethod = method;
            }
        }

        if (method != null) {
            try {
                Object result = method.invoke(null, texture);
                if (result instanceof RenderLayer renderLayer) {
                    return renderLayer;
                }
            } catch (ReflectiveOperationException ignored) {
                if (cacheEntityField) {
                    cachedEntityTextureMethod = null;
                } else {
                    cachedTextLayerMethod = null;
                }
            }
        }

        return null;
    }

    private static Method resolveLayerMethod(List<String> newApiNames, List<String> legacyApiNames) {
        Method method = findLayerMethod(loadNewLayerHostClass(), newApiNames);
        if (method != null) {
            return method;
        }
        return findLayerMethod(RenderLayer.class, legacyApiNames);
    }

    private static Class<?> loadNewLayerHostClass() {
        Class<?> host = cachedLayerHostClass;
        if (host != null) {
            return host;
        }
        try {
            host = Class.forName("net.minecraft.client.render.RenderLayers");
            cachedLayerHostClass = host;
            return host;
        } catch (ClassNotFoundException ignored) {
            cachedLayerHostClass = RenderLayer.class;
            return RenderLayer.class;
        }
    }

    private static Method findLayerMethod(Class<?> host, List<String> preferredNames) {
        Method[] methods = host.getMethods();
        for (String preferredName : preferredNames) {
            for (Method method : methods) {
                if (isMatchingLayerMethod(method) && preferredName.equals(method.getName())) {
                    return method;
                }
            }
        }

        for (Method method : methods) {
            if (isMatchingLayerMethod(method)) {
                return method;
            }
        }

        return null;
    }

    private static boolean isMatchingLayerMethod(Method method) {
        return Modifier.isStatic(method.getModifiers())
            && method.getReturnType() == RenderLayer.class
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == Identifier.class;
    }
}
