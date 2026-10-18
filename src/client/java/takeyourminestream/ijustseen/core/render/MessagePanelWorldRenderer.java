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

    /** 3D-рамка сообщения: выпуклый бортик + задняя стенка. */
    public static void drawPanelWithBorder(
        MatrixStack matrices,
        net.minecraft.client.render.VertexConsumerProvider consumers,
        int x,
        int y,
        int width,
        int height,
        float alpha,
        int borderRgb
    ) {
        int baseRgb = takeyourminestream.ijustseen.core.MessagePanelColors.baseRgb();
        int resolvedBorderRgb = takeyourminestream.ijustseen.core.MessagePanelColors.borderRgb(borderRgb);

        if (takeyourminestream.ijustseen.config.ModConfig.isMESSAGE_3D_FRAME()) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            int light = 0xF000F0;
            int overlay = OverlayTexture.DEFAULT_UV;

            java.util.List<MessageFrame3DModel.FrameQuad> quads = MessageFrame3DModel.getInstance().buildQuads(
                x, y, width, height, alpha, baseRgb, resolvedBorderRgb
            );

            for (MessageFrame3DModel.FrameQuad q : quads) {
                VertexConsumer consumer = RenderLayerCompat.getEntityBuffer(consumers, q.textureId());
                consumer.vertex(matrix, q.x0(), q.y0(), q.z0())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u0(), q.v0()).overlay(overlay).light(light).normal(0, 0, -1);
                consumer.vertex(matrix, q.x1(), q.y1(), q.z1())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u1(), q.v1()).overlay(overlay).light(light).normal(0, 0, -1);
                consumer.vertex(matrix, q.x2(), q.y2(), q.z2())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u2(), q.v2()).overlay(overlay).light(light).normal(0, 0, -1);
                consumer.vertex(matrix, q.x3(), q.y3(), q.z3())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u3(), q.v3()).overlay(overlay).light(light).normal(0, 0, -1);
            }
        } else {
            // Классический плоский 2D-режим (как было до этого)
            VertexConsumer base = RenderLayerCompat.getEntityBuffer(consumers, MessagePanelConstants.PANEL_TEXTURE);
            drawPanel(
                matrices,
                base,
                x,
                y,
                width,
                height,
                alpha,
                ((baseRgb >> 16) & 0xFF) / 255.0f,
                ((baseRgb >> 8) & 0xFF) / 255.0f,
                (baseRgb & 0xFF) / 255.0f
            );

            float red = ((resolvedBorderRgb >> 16) & 0xFF) / 255.0f;
            float green = ((resolvedBorderRgb >> 8) & 0xFF) / 255.0f;
            float blue = (resolvedBorderRgb & 0xFF) / 255.0f;
            VertexConsumer border = RenderLayerCompat.getEntityBuffer(consumers, MessagePanelConstants.PANEL_BORDER_TEXTURE);
            matrices.push();
            matrices.translate(0f, 0f, 0.02f);
            drawPanel(matrices, border, x, y, width, height, alpha, red, green, blue);
            matrices.pop();
        }
    }

    /** Каркас куба 1×1×1 блок с центром в начале текущей матрицы (единицы — блоки). */
    public static void drawBlockCube(Matrix4f matrix, net.minecraft.client.render.VertexConsumerProvider consumers) {
        VertexConsumer consumer = RenderLayerCompat.getEntityBuffer(consumers, MessagePanelConstants.EDITOR_CUBE_TEXTURE);
        int light = 0xF000F0;
        int overlay = OverlayTexture.DEFAULT_UV;
        for (BlockCubeGeometry.Quad quad : BlockCubeGeometry.quads()) {
            float alpha = quad.edge() ? BlockCubeGeometry.EDGE_ALPHA : BlockCubeGeometry.FACE_ALPHA;
            float[] p = quad.xyz();
            for (int i = 0; i < 4; i++) {
                consumer.vertex(matrix, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                    .color(1f, 1f, 1f, alpha).texture(0.5f, 0.5f).overlay(overlay).light(light).normal(0, 0, -1);
            }
        }
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
