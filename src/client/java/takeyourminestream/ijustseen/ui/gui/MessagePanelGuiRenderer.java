package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.render.MessagePanel9Slice;

/** Рендер 9-slice панели сообщений в GUI. */
public final class MessagePanelGuiRenderer {
    private MessagePanelGuiRenderer() {}

    public static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        drawPanel(context, x, y, width, height, 1.0f);
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height, float alpha) {
        int color = alpha >= 1.0f ? -1 : ((int) (alpha * 255.0f) << 24) | 0xFFFFFF;
        for (MessagePanel9Slice.GuiSlice slice : MessagePanel9Slice.guiSlices(x, y, width, height)) {
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                MessagePanelConstants.PANEL_TEXTURE,
                slice.x(),
                slice.y(),
                slice.texU(),
                slice.texV(),
                slice.width(),
                slice.height(),
                slice.regionWidth(),
                slice.regionHeight(),
                MessagePanelConstants.TEX_SIZE,
                MessagePanelConstants.TEX_SIZE,
                color
            );
        }
    }
}
