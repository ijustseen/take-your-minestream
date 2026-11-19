package takeyourminestream.ijustseen.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.utils.SubmitGeometryHelper;

/** 9-slice панель сообщений в 3D-мире (Minecraft 26.2). */
public final class MessagePanelWorldRenderer {
    /**
     * Бортик чуть ближе к камере, чем заливка: в 26.2 translucent стоят в одну
     * плоскость и сортировка COLLECT_SUBMITS иногда рисует фон поверх рамки.
     */
    private static final float BORDER_Z = 0.02f;

    private MessagePanelWorldRenderer() {}

    /** 3D-рамка сообщения: выпуклый бортик + задняя стенка. */
    public static void drawPanelWithBorder(
        PoseStack poseStack,
        SubmitNodeCollector collector,
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
            java.util.List<takeyourminestream.ijustseen.core.render.MessageFrame3DModel.FrameQuad> quads =
                takeyourminestream.ijustseen.core.render.MessageFrame3DModel.getInstance().buildQuads(
                    x, y, width, height, alpha, baseRgb, resolvedBorderRgb
                );
            SubmitGeometryHelper.submitFrameQuads(collector, poseStack, quads);
        } else {
            SubmitGeometryHelper.submitPanel(
                collector,
                poseStack,
                takeyourminestream.ijustseen.core.render.MessagePanelTextures.panel(),
                x,
                y,
                width,
                height,
                0f,
                alpha,
                ((baseRgb >> 16) & 0xFF) / 255.0f,
                ((baseRgb >> 8) & 0xFF) / 255.0f,
                (baseRgb & 0xFF) / 255.0f
            );

            float red = ((resolvedBorderRgb >> 16) & 0xFF) / 255.0f;
            float green = ((resolvedBorderRgb >> 8) & 0xFF) / 255.0f;
            float blue = (resolvedBorderRgb & 0xFF) / 255.0f;
            SubmitGeometryHelper.submitPanel(
                collector,
                poseStack,
                takeyourminestream.ijustseen.core.render.MessagePanelTextures.border(),
                x,
                y,
                width,
                height,
                BORDER_Z,
                alpha,
                red,
                green,
                blue
            );
        }
    }

    /** Дешёвый LOD: fill + тонкая рамка без 9-slice/3D. */
    public static void drawSimplifiedPanel(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int x,
        int y,
        int width,
        int height,
        float alpha,
        int fillRgb,
        int borderRgb
    ) {
        float fr = ((fillRgb >> 16) & 0xFF) / 255.0f;
        float fg = ((fillRgb >> 8) & 0xFF) / 255.0f;
        float fb = (fillRgb & 0xFF) / 255.0f;
        float br = ((borderRgb >> 16) & 0xFF) / 255.0f;
        float bg = ((borderRgb >> 8) & 0xFF) / 255.0f;
        float bb = (borderRgb & 0xFF) / 255.0f;
        var panel = MessagePanelTextures.panel();
        var border = MessagePanelTextures.border();
        SubmitGeometryHelper.submitTexturedQuad(
            collector, poseStack, panel, false,
            x, y, x + width, y + height, 0f,
            0.5f, 0.5f, 0.5f, 0.5f, fr, fg, fb, alpha
        );
        float e = 1.0f;
        SubmitGeometryHelper.submitTexturedQuad(collector, poseStack, border, false, x, y, x + width, y + e, BORDER_Z, 0.5f, 0.5f, 0.5f, 0.5f, br, bg, bb, alpha);
        SubmitGeometryHelper.submitTexturedQuad(collector, poseStack, border, false, x, y + height - e, x + width, y + height, BORDER_Z, 0.5f, 0.5f, 0.5f, 0.5f, br, bg, bb, alpha);
        SubmitGeometryHelper.submitTexturedQuad(collector, poseStack, border, false, x, y, x + e, y + height, BORDER_Z, 0.5f, 0.5f, 0.5f, 0.5f, br, bg, bb, alpha);
        SubmitGeometryHelper.submitTexturedQuad(collector, poseStack, border, false, x + width - e, y, x + width, y + height, BORDER_Z, 0.5f, 0.5f, 0.5f, 0.5f, br, bg, bb, alpha);
    }

    /** Каркас куба 1×1×1 блок с центром в начале текущей матрицы (единицы — блоки). */
    public static void drawBlockCube(org.joml.Matrix4f matrix, SubmitNodeCollector collector) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(matrix);
        SubmitGeometryHelper.submitBlockCube(collector, poseStack, MessagePanelConstants.EDITOR_CUBE_TEXTURE);
    }
}
