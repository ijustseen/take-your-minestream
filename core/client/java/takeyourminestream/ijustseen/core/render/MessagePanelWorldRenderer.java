package takeyourminestream.ijustseen.core.render;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.utils.RenderLayerCompat;

/**
 * 9-slice панель сообщений в 3D-мире.
 *
 * <p>Рисуется по одной текстуре за вызов: смена слоя в {@code Immediate} сбрасывает предыдущий
 * слой на GPU, поэтому рендерер сначала выводит заливку всех сообщений, затем бортики и т.д.
 */
public final class MessagePanelWorldRenderer {
    private static final int LIGHT = 0xF000F0;

    private MessagePanelWorldRenderer() {}

    /** Текстуры панели в порядке отрисовки; задник нужен только 3D-рамке. */
    public static Identifier[] layerTextures() {
        if (takeyourminestream.ijustseen.config.ModConfig.isMESSAGE_3D_FRAME()) {
            return new Identifier[] {
                MessagePanelTextures.panel(),
                MessagePanelTextures.border(),
                MessagePanelConstants.FRAME_BACK_TEXTURE
            };
        }
        return new Identifier[] {MessagePanelTextures.panel(), MessagePanelTextures.border()};
    }

    /** Вся панель одного сообщения целиком (без пакетной отрисовки по слоям). */
    public static void drawPanelWithBorder(
        net.minecraft.client.util.math.MatrixStack matrices,
        VertexConsumerProvider consumers,
        int x,
        int y,
        int width,
        int height,
        float alpha,
        int borderRgb
    ) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (Identifier texture : layerTextures()) {
            drawPanelLayer(matrix, consumers, texture, x, y, width, height, alpha, borderRgb);
        }
    }

    /** Часть полной панели (3D-рамка или плоский 9-slice), относящаяся к {@code texture}. */
    public static void drawPanelLayer(
        Matrix4f matrix,
        VertexConsumerProvider consumers,
        Identifier texture,
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
            int overlay = OverlayTexture.DEFAULT_UV;
            java.util.List<MessageFrame3DModel.FrameQuad> quads = MessageFrame3DModel.getInstance().buildQuads(
                x, y, width, height, alpha, baseRgb, resolvedBorderRgb
            );
            VertexConsumer consumer = null;
            for (MessageFrame3DModel.FrameQuad q : quads) {
                if (!q.textureId().equals(texture)) {
                    continue;
                }
                if (consumer == null) {
                    consumer = RenderLayerCompat.getEntityBuffer(consumers, texture);
                }
                consumer.vertex(matrix, q.x0(), q.y0(), q.z0())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u0(), q.v0()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
                consumer.vertex(matrix, q.x1(), q.y1(), q.z1())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u1(), q.v1()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
                consumer.vertex(matrix, q.x2(), q.y2(), q.z2())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u2(), q.v2()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
                consumer.vertex(matrix, q.x3(), q.y3(), q.z3())
                    .color(q.r(), q.g(), q.b(), q.a()).texture(q.u3(), q.v3()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
            }
            return;
        }

        if (texture.equals(MessagePanelTextures.panel())) {
            drawPanel(
                matrix,
                RenderLayerCompat.getEntityBuffer(consumers, texture),
                x, y, width, height, 0f, alpha,
                ((baseRgb >> 16) & 0xFF) / 255.0f,
                ((baseRgb >> 8) & 0xFF) / 255.0f,
                (baseRgb & 0xFF) / 255.0f
            );
        } else if (texture.equals(MessagePanelTextures.border())) {
            drawPanel(
                matrix,
                RenderLayerCompat.getEntityBuffer(consumers, texture),
                x, y, width, height, 0.02f, alpha,
                ((resolvedBorderRgb >> 16) & 0xFF) / 255.0f,
                ((resolvedBorderRgb >> 8) & 0xFF) / 255.0f,
                (resolvedBorderRgb & 0xFF) / 255.0f
            );
        }
    }

    /**
     * Часть дешёвого LOD-билборда для {@code texture}: заливка — один quad, рамка — 4 полоски.
     * Координаты — в локальной плоскости сообщения (как у полной панели).
     */
    public static void drawSimplifiedPanelLayer(
        Matrix4f matrix,
        VertexConsumerProvider consumers,
        Identifier texture,
        int x,
        int y,
        int width,
        int height,
        float alpha,
        int fillRgb,
        int borderRgb
    ) {
        if (texture.equals(MessagePanelTextures.panel())) {
            VertexConsumer fill = RenderLayerCompat.getEntityBuffer(consumers, texture);
            flatQuad(fill, matrix, x, y, x + width, y + height, 0f, fillRgb, alpha);
        } else if (texture.equals(MessagePanelTextures.border())) {
            float edge = 1.0f;
            float z = 0.01f;
            VertexConsumer border = RenderLayerCompat.getEntityBuffer(consumers, texture);
            flatQuad(border, matrix, x, y, x + width, y + edge, z, borderRgb, alpha);
            flatQuad(border, matrix, x, y + height - edge, x + width, y + height, z, borderRgb, alpha);
            flatQuad(border, matrix, x, y, x + edge, y + height, z, borderRgb, alpha);
            flatQuad(border, matrix, x + width - edge, y, x + width, y + height, z, borderRgb, alpha);
        }
    }

    /** Каркас куба 1×1×1 блок с центром в начале текущей матрицы (единицы — блоки). */
    public static void drawBlockCube(Matrix4f matrix, VertexConsumerProvider consumers) {
        VertexConsumer consumer = RenderLayerCompat.getEntityBuffer(consumers, MessagePanelConstants.EDITOR_CUBE_TEXTURE);
        int overlay = OverlayTexture.DEFAULT_UV;
        for (BlockCubeGeometry.Quad quad : BlockCubeGeometry.quads()) {
            float alpha = quad.edge() ? BlockCubeGeometry.EDGE_ALPHA : BlockCubeGeometry.FACE_ALPHA;
            float[] p = quad.xyz();
            for (int i = 0; i < 4; i++) {
                consumer.vertex(matrix, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                    .color(1f, 1f, 1f, alpha).texture(0.5f, 0.5f).overlay(overlay).light(LIGHT).normal(0, 0, -1);
            }
        }
    }

    private static void drawPanel(
        Matrix4f matrix,
        VertexConsumer consumer,
        int x,
        int y,
        int width,
        int height,
        float z,
        float alpha,
        float red,
        float green,
        float blue
    ) {
        int overlay = OverlayTexture.DEFAULT_UV;
        for (MessagePanel9Slice.WorldSlice slice : MessagePanel9Slice.worldSlices(x, y, width, height)) {
            consumer.vertex(matrix, slice.x0(), slice.y0(), z)
                .color(red, green, blue, alpha).texture(slice.u0(), slice.v0()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
            consumer.vertex(matrix, slice.x0(), slice.y1(), z)
                .color(red, green, blue, alpha).texture(slice.u0(), slice.v1()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
            consumer.vertex(matrix, slice.x1(), slice.y1(), z)
                .color(red, green, blue, alpha).texture(slice.u1(), slice.v1()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
            consumer.vertex(matrix, slice.x1(), slice.y0(), z)
                .color(red, green, blue, alpha).texture(slice.u1(), slice.v0()).overlay(overlay).light(LIGHT).normal(0, 0, -1);
        }
    }

    private static void flatQuad(
        VertexConsumer consumer,
        Matrix4f matrix,
        float x0,
        float y0,
        float x1,
        float y1,
        float z,
        int rgb,
        float alpha
    ) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        int overlay = OverlayTexture.DEFAULT_UV;
        consumer.vertex(matrix, x0, y0, z).color(r, g, b, alpha).texture(0.5f, 0.5f).overlay(overlay).light(LIGHT).normal(0, 0, -1);
        consumer.vertex(matrix, x0, y1, z).color(r, g, b, alpha).texture(0.5f, 0.5f).overlay(overlay).light(LIGHT).normal(0, 0, -1);
        consumer.vertex(matrix, x1, y1, z).color(r, g, b, alpha).texture(0.5f, 0.5f).overlay(overlay).light(LIGHT).normal(0, 0, -1);
        consumer.vertex(matrix, x1, y0, z).color(r, g, b, alpha).texture(0.5f, 0.5f).overlay(overlay).light(LIGHT).normal(0, 0, -1);
    }
}
