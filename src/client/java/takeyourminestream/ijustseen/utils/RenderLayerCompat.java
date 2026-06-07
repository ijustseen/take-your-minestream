package takeyourminestream.ijustseen.utils;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
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
        var layer = resolveEntityTextureLayer(texture);
        if (layer == null) {
            throw new IllegalStateException("Unable to resolve entity RenderLayer for texture: " + texture);
        }
        return layer;
    }

    public static RenderLayer getTextLayer(Identifier texture) {
        var textLayer = invokeLayerMethod(cachedTextLayerMethod, texture, false, false);
        if (textLayer != null) {
            return textLayer;
        }
        textLayer = fallbackTextLayer(texture);
        if (textLayer != null) {
            return textLayer;
        }
        return getEntityTextureLayer(texture);
    }

    public static VertexConsumer getEntityBuffer(VertexConsumerProvider consumers, Identifier texture) {
        return consumers.getBuffer(getEntityTextureLayer(texture));
    }

    public static VertexConsumer getTextBuffer(VertexConsumerProvider consumers, Identifier texture) {
        return consumers.getBuffer(getTextLayer(texture));
    }

    private static RenderLayer resolveEntityTextureLayer(Identifier texture) {
        var layer = invokeLayerMethod(cachedEntityTextureMethod, texture, true, true);
        if (layer != null) {
            return layer;
        }
        layer = fallbackEntityTextureLayer(texture);
        if (layer != null) {
            return layer;
        }
        return tryAllLayerMethods(loadNewLayerHostClass(), texture);
    }

    private static RenderLayer invokeLayerMethod(
        Method cached,
        Identifier texture,
        boolean cacheEntityField,
        boolean entityMethods
    ) {
        Method method = cached;
        if (method == null) {
            synchronized (RenderLayerCompat.class) {
                method = cacheEntityField ? cachedEntityTextureMethod : cachedTextLayerMethod;
                if (method == null) {
                    List<String> newNames = entityMethods ? NEW_ENTITY_METHOD_NAMES : NEW_TEXT_METHOD_NAMES;
                    List<String> legacyNames = entityMethods ? LEGACY_ENTITY_METHOD_NAMES : LEGACY_TEXT_METHOD_NAMES;
                    method = resolveLayerMethod(newNames, legacyNames);
                    if (method != null) {
                        if (cacheEntityField) {
                            cachedEntityTextureMethod = method;
                        } else {
                            cachedTextLayerMethod = method;
                        }
                    }
                }
            }
        }

        if (method != null) {
            var layer = invokeResolvedLayerMethod(method, texture);
            if (layer != null) {
                return layer;
            }
            synchronized (RenderLayerCompat.class) {
                if (cacheEntityField) {
                    cachedEntityTextureMethod = null;
                } else {
                    cachedTextLayerMethod = null;
                }
            }
        }

        return null;
    }

    private static RenderLayer fallbackEntityTextureLayer(Identifier texture) {
        var layer = tryInvokeNamedMethods(loadNewLayerHostClass(), texture, NEW_ENTITY_METHOD_NAMES);
        if (layer != null) {
            return layer;
        }
        layer = tryInvokeNamedMethods(RenderLayer.class, texture, LEGACY_ENTITY_METHOD_NAMES);
        if (layer != null) {
            return layer;
        }
        layer = tryInvokeNamedMethods(RenderLayer.class, texture, NEW_ENTITY_METHOD_NAMES);
        if (layer != null) {
            return layer;
        }
        layer = tryInvokeNamedMethods(RenderLayer.class, texture, List.of("getEntitySolid", "entitySolid"));
        if (layer != null) {
            return layer;
        }
        return tryAllLayerMethods(RenderLayer.class, texture);
    }

    private static RenderLayer fallbackTextLayer(Identifier texture) {
        var layer = tryInvokeNamedMethods(loadNewLayerHostClass(), texture, NEW_TEXT_METHOD_NAMES);
        if (layer != null) {
            return layer;
        }
        layer = tryInvokeNamedMethods(RenderLayer.class, texture, LEGACY_TEXT_METHOD_NAMES);
        if (layer != null) {
            return layer;
        }
        return tryAllLayerMethods(loadNewLayerHostClass(), texture);
    }

    private static RenderLayer tryAllLayerMethods(Class<?> host, Identifier texture) {
        Method[] methods = host.getMethods();
        for (String preferredName : NEW_ENTITY_METHOD_NAMES) {
            for (Method method : methods) {
                if (isMatchingLayerMethod(method) && preferredName.equals(method.getName())) {
                    var layer = invokeResolvedLayerMethod(method, texture);
                    if (layer != null) {
                        return layer;
                    }
                }
            }
        }
        for (Method method : methods) {
            if (isMatchingLayerMethod(method)) {
                var layer = invokeResolvedLayerMethod(method, texture);
                if (layer != null) {
                    return layer;
                }
            }
        }
        return null;
    }

    private static RenderLayer tryInvokeNamedMethods(Class<?> host, Identifier texture, List<String> names) {
        for (String name : names) {
            var layer = tryInvokeNamedMethod(host, name, texture);
            if (layer != null) {
                return layer;
            }
        }
        return null;
    }

    private static RenderLayer tryInvokeNamedMethod(Class<?> host, String name, Identifier texture) {
        try {
            Method method = host.getMethod(name, Identifier.class);
            return invokeResolvedLayerMethod(method, texture);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static RenderLayer invokeResolvedLayerMethod(Method method, Identifier texture) {
        try {
            Object result = method.invoke(null, texture);
            if (result instanceof RenderLayer renderLayer) {
                return renderLayer;
            }
        } catch (ReflectiveOperationException ignored) {
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
        synchronized (RenderLayerCompat.class) {
            host = cachedLayerHostClass;
            if (host != null) {
                return host;
            }
            try {
                host = Class.forName("net.minecraft.client.render.RenderLayers");
            } catch (ClassNotFoundException ignored) {
                host = RenderLayer.class;
            }
            cachedLayerHostClass = host;
            return host;
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

        return null;
    }

    private static boolean isMatchingLayerMethod(Method method) {
        return Modifier.isStatic(method.getModifiers())
            && RenderLayer.class.isAssignableFrom(method.getReturnType())
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == Identifier.class;
    }
}
