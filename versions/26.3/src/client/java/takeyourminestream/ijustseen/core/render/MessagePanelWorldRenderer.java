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

    /** Панель: фон + бортик, тонированный цветом платформы. */
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
        SubmitGeometryHelper.submitPanel(
            collector,
            poseStack,
            MessagePanelConstants.PANEL_TEXTURE,
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

        int resolvedBorderRgb = takeyourminestream.ijustseen.core.MessagePanelColors.borderRgb(borderRgb);
        float red = ((resolvedBorderRgb >> 16) & 0xFF) / 255.0f;
        float green = ((resolvedBorderRgb >> 8) & 0xFF) / 255.0f;
        float blue = (resolvedBorderRgb & 0xFF) / 255.0f;
        SubmitGeometryHelper.submitPanel(
            collector,
            poseStack,
            MessagePanelConstants.PANEL_BORDER_TEXTURE,
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
