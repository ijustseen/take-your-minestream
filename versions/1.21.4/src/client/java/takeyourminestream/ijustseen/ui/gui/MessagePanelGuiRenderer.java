package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.render.MessagePanel9Slice;

/** Рендер 9-slice панели в GUI (Minecraft 1.21.4 — RenderLayer API). */
public final class MessagePanelGuiRenderer {
    private MessagePanelGuiRenderer() {}

    public static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        drawPanel(context, x, y, width, height, 1.0f);
    }

    public static void drawPinIcon(DrawContext context, int panelX, int panelY, int panelWidth, float alpha) {
        int pinX = panelX + panelWidth - MessagePanelConstants.PADDING_X
            - (MessagePanelConstants.PIN_ICON_SIZE / 2)
            + MessagePanelConstants.PIN_ICON_MARGIN;
        int pinY = panelY - (MessagePanelConstants.PIN_ICON_SIZE / 2) - MessagePanelConstants.PIN_ICON_MARGIN;
        int color = alpha >= 1.0f ? -1 : ((int) (alpha * 255.0f) << 24) | 0xFFFFFF;
        context.drawTexture(
            RenderLayer::getGuiTextured,
            MessagePanelConstants.PIN_TEXTURE,
            pinX,
            pinY,
            0,
            0,
            MessagePanelConstants.PIN_ICON_SIZE,
            MessagePanelConstants.PIN_ICON_SIZE,
            MessagePanelConstants.PIN_ICON_SIZE,
            MessagePanelConstants.PIN_ICON_SIZE,
            MessagePanelConstants.PIN_ICON_SIZE,
            MessagePanelConstants.PIN_ICON_SIZE,
            color
        );
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height, float alpha) {
        int color = alpha >= 1.0f ? -1 : ((int) (alpha * 255.0f) << 24) | 0xFFFFFF;
        for (MessagePanel9Slice.GuiSlice slice : MessagePanel9Slice.guiSlices(x, y, width, height)) {
            context.drawTexture(
                RenderLayer::getGuiTextured,
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
