package takeyourminestream.ijustseen.utils;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** RenderType-хелпер (26.2 использует SubmitNodeCollector, буферы не нужны). */
public final class RenderLayerCompat {
    private RenderLayerCompat() {}

    public static RenderType getEntityTextureLayer(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType getTextLayer(Identifier texture) {
        return RenderTypes.text(texture);
    }
}
