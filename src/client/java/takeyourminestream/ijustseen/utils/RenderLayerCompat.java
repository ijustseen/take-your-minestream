package takeyourminestream.ijustseen.utils;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

public final class RenderLayerCompat {
    private RenderLayerCompat() {
    }

    public static RenderLayer getEntityTextureLayer(Identifier texture) {
        return RenderLayers.entityTranslucent(texture);
    }

    public static RenderLayer tryGetEntityTextureLayer(Identifier texture) {
        return RenderLayers.entityTranslucent(texture);
    }
}