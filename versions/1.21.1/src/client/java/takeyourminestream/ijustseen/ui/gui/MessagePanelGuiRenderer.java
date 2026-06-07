package takeyourminestream.ijustseen.ui.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.render.MessagePanel9Slice;

/** Рендер 9-slice панели в GUI (Minecraft 1.21.1 — legacy drawTexture). */
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
        drawTextured(context, MessagePanelConstants.PIN_TEXTURE, pinX, pinY,
            MessagePanelConstants.PIN_ICON_SIZE, MessagePanelConstants.PIN_ICON_SIZE, alpha);
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height, float alpha) {
        for (MessagePanel9Slice.GuiSlice slice : MessagePanel9Slice.guiSlices(x, y, width, height)) {
            drawTextured(context, MessagePanelConstants.PANEL_TEXTURE,
                slice.x(), slice.y(),
                slice.width(), slice.height(),
                slice.texU(), slice.texV(),
                slice.regionWidth(), slice.regionHeight(),
                alpha);
        }
    }

    private static void drawTextured(
        DrawContext context,
        net.minecraft.util.Identifier texture,
        int x,
        int y,
        int width,
        int height,
        float alpha
    ) {
        drawTextured(context, texture, x, y, width, height, 0f, 0f, width, height, alpha);
    }

    private static void drawTextured(
        DrawContext context,
        net.minecraft.util.Identifier texture,
        int x,
        int y,
        int width,
        int height,
        float u,
        float v,
        int regionWidth,
        int regionHeight,
        float alpha
    ) {
        if (alpha < 1.0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }
        context.drawTexture(
            texture,
            x,
            y,
            width,
            height,
            u,
            v,
            regionWidth,
            regionHeight,
            MessagePanelConstants.TEX_SIZE,
            MessagePanelConstants.TEX_SIZE
        );
        if (alpha < 1.0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}
