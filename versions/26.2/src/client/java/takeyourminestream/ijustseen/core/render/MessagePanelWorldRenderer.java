package takeyourminestream.ijustseen.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.utils.SubmitGeometryHelper;

/** 9-slice панель сообщений в 3D-мире (Minecraft 26.2). */
public final class MessagePanelWorldRenderer {
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
        SubmitGeometryHelper.submitPanel(
            collector,
            poseStack,
            MessagePanelConstants.PANEL_BASE_TEXTURE,
            x,
            y,
            width,
            height,
            alpha,
            1.0f,
            1.0f,
            1.0f
        );

        float red = ((borderRgb >> 16) & 0xFF) / 255.0f;
        float green = ((borderRgb >> 8) & 0xFF) / 255.0f;
        float blue = (borderRgb & 0xFF) / 255.0f;
        SubmitGeometryHelper.submitPanel(
            collector,
            poseStack,
            MessagePanelConstants.PANEL_BORDER_TEXTURE,
            x,
            y,
            width,
            height,
            alpha,
            red,
            green,
            blue
        );
    }
}
