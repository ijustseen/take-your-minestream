package takeyourminestream.ijustseen.utils;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;

public final class RenderLayerCompat {
    private RenderLayerCompat() {
    }

    public static RenderLayer getEntityTextureLayer(Identifier texture) {
        return RenderLayers.entityTranslucent(texture);
    }

    public static RenderLayer getTextLayer(Identifier texture) {
        return RenderLayers.text(texture);
    }

    public static VertexConsumer getEntityBuffer(VertexConsumerProvider consumers, Identifier texture) {
        return consumers.getBuffer(getEntityTextureLayer(texture));
    }

    public static VertexConsumer getTextBuffer(VertexConsumerProvider consumers, Identifier texture) {
        return consumers.getBuffer(getTextLayer(texture));
    }
}
