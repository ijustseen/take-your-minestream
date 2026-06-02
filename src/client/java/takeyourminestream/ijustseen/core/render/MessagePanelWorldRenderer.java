package takeyourminestream.ijustseen.core.render;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.utils.RenderLayerCompat;

/** 9-slice панель сообщений в 3D-мире. */
public final class MessagePanelWorldRenderer {
    private MessagePanelWorldRenderer() {}

    public static void drawPanel(
        MatrixStack matrices,
        VertexConsumer consumer,
        int x,
        int y,
        int width,
        int height,
        float alpha,
        float red,
        float green,
        float blue
    ) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (MessagePanel9Slice.WorldSlice slice : MessagePanel9Slice.worldSlices(x, y, width, height)) {
            drawQuad(consumer, matrix, slice, alpha, red, green, blue);
        }
    }

    public static VertexConsumer panelConsumer(net.minecraft.client.render.VertexConsumerProvider consumers) {
        return consumers.getBuffer(RenderLayerCompat.getEntityTextureLayer(MessagePanelConstants.PANEL_TEXTURE));
    }

    private static void drawQuad(
        VertexConsumer consumer,
        Matrix4f matrix,
        MessagePanel9Slice.WorldSlice slice,
        float alpha,
        float red,
        float green,
        float blue
    ) {
        int light = 0xF000F0;
        int overlay = OverlayTexture.DEFAULT_UV;

        consumer.vertex(matrix, slice.x0(), slice.y0(), 0)
            .color(red, green, blue, alpha).texture(slice.u0(), slice.v0()).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(matrix, slice.x0(), slice.y1(), 0)
            .color(red, green, blue, alpha).texture(slice.u0(), slice.v1()).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(matrix, slice.x1(), slice.y1(), 0)
            .color(red, green, blue, alpha).texture(slice.u1(), slice.v1()).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(matrix, slice.x1(), slice.y0(), 0)
            .color(red, green, blue, alpha).texture(slice.u1(), slice.v0()).overlay(overlay).light(light).normal(0, 0, -1);
    }
}
