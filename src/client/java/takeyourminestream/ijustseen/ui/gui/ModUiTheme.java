package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Единый визуальный стиль GUI мода (как выпадающее меню истории). */
public final class ModUiTheme {
    public static final int PANEL_BG = 0xE0101010;
    public static final int PANEL_BORDER = 0xFF555555;
    public static final int BUTTON_BG = 0xFF3A3A3A;
    public static final int BUTTON_BG_HOVER = 0xFF525252;
    public static final int BUTTON_BORDER_HOVER = 0xFF909090;
    public static final int BUTTON_BG_SELECTED = 0xFF5E5E5E;
    public static final int ROW_BG = 0x40282828;
    public static final int ROW_BG_HOVER = 0x90404040;
    public static final int INPUT_BG = 0x90202020;
    public static final int DANGER_BG = 0x90604040;
    public static final int DANGER_BG_HOVER = 0xC0805050;
    public static final int ACCENT_BG = 0x90484868;
    public static final int ACCENT_BG_HOVER = 0xC0686888;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFDDDDDD;
    public static final int TEXT_MUTED = 0xFFAAAAAA;
    public static final int TEXT_DIM = 0xFF666666;
    public static final int TEXT_HINT = 0xFF888888;
    public static final int OVERLAY_DIM = 0x60000000;
    /** Fully opaque panel for modal popups (avoids batched text bleeding through on 1.21). */
    public static final int POPUP_PANEL_BG = 0xFF101010;
    public static final int SCROLL_TRACK = 0x40555555;
    public static final int SCROLL_THUMB = 0xC0686868;

    public enum ButtonVariant {
        NEUTRAL,
        DANGER,
        ACCENT
    }

    private ModUiTheme() {}

    public static void drawBorderedPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL_BG);
        context.fill(x, y, x + width, y + 1, PANEL_BORDER);
        context.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        context.fill(x, y, x + 1, y + height, PANEL_BORDER);
        context.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    public static void drawButtonWidget(
        DrawContext context,
        TextRenderer textRenderer,
        ButtonWidget button,
        boolean selected
    ) {
        drawButton(
            context,
            textRenderer,
            button.getX(),
            button.getY(),
            button.getWidth(),
            button.getHeight(),
            button.getMessage(),
            button.isHovered(),
            button.active,
            selected,
            true
        );
    }

    public static void drawButton(
        DrawContext context,
        TextRenderer textRenderer,
        int x,
        int y,
        int width,
        int height,
        Text label,
        boolean hovered,
        boolean enabled,
        boolean selected
    ) {
        drawButton(context, textRenderer, x, y, width, height, label, hovered, enabled, selected, true);
    }

    public static void drawButton(
        DrawContext context,
        TextRenderer textRenderer,
        int x,
        int y,
        int width,
        int height,
        Text label,
        boolean hovered,
        boolean enabled,
        boolean selected,
        boolean hoverBorders
    ) {
        drawButtonInternal(context, textRenderer, x, y, width, height, label, hovered, enabled, selected, hoverBorders, false);
    }

    /** Popup menu button — uses overlay render layer on Minecraft 1.21. */
    public static void drawPopupButton(
        DrawContext context,
        TextRenderer textRenderer,
        int x,
        int y,
        int width,
        int height,
        Text label,
        boolean hovered,
        boolean enabled,
        boolean selected
    ) {
        drawButtonInternal(context, textRenderer, x, y, width, height, label, hovered, enabled, selected, true, true);
    }

    private static void drawButtonInternal(
        DrawContext context,
        TextRenderer textRenderer,
        int x,
        int y,
        int width,
        int height,
        Text label,
        boolean hovered,
        boolean enabled,
        boolean selected,
        boolean hoverBorders,
        boolean overlayLayer
    ) {
        int bg = selected ? BUTTON_BG_SELECTED : (hovered && enabled ? BUTTON_BG_HOVER : BUTTON_BG);
        if (!enabled) {
            bg = 0x60404040;
        }
        if (overlayLayer) {
            PopupGuiRenderer.fill(context, x, y, x + width, y + height, bg);
            if (hovered && enabled && hoverBorders) {
                PopupGuiRenderer.fill(context, x, y, x + width, y + 1, BUTTON_BORDER_HOVER);
                PopupGuiRenderer.fill(context, x, y + height - 1, x + width, y + height, BUTTON_BORDER_HOVER);
            }
        } else {
            context.fill(x, y, x + width, y + height, bg);
            if (hovered && enabled && hoverBorders) {
                context.fill(x, y, x + width, y + 1, BUTTON_BORDER_HOVER);
                context.fill(x, y + height - 1, x + width, y + height, BUTTON_BORDER_HOVER);
            }
        }
        int textColor = enabled ? (hovered || selected ? TEXT_PRIMARY : TEXT_SECONDARY) : TEXT_HINT;
        int labelWidth = textRenderer.getWidth(label);
        int textX = x + Math.max(6, (width - labelWidth) / 2);
        int textY = centeredTextY(y, height, textRenderer.fontHeight);
        context.drawTextWithShadow(textRenderer, label, textX, textY, textColor);
    }

    public static void drawCompactButton(
        DrawContext context,
        TextRenderer textRenderer,
        int x,
        int y,
        int width,
        int height,
        Text label,
        boolean hovered,
        ButtonVariant variant
    ) {
        int bg = switch (variant) {
            case DANGER -> hovered ? DANGER_BG_HOVER : DANGER_BG;
            case ACCENT -> hovered ? ACCENT_BG_HOVER : ACCENT_BG;
            case NEUTRAL -> hovered ? BUTTON_BG_HOVER : BUTTON_BG;
        };
        context.fill(x, y, x + width, y + height, bg);
        if (hovered) {
            context.fill(x, y, x + width, y + 1, BUTTON_BORDER_HOVER);
            context.fill(x, y + height - 1, x + width, y + height, BUTTON_BORDER_HOVER);
        }
        int labelWidth = textRenderer.getWidth(label);
        int textX = x + (width - labelWidth) / 2;
        int textY = centeredTextY(y, height, textRenderer.fontHeight);
        context.drawTextWithShadow(textRenderer, label, textX, textY, hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
    }

    public static void drawListRow(DrawContext context, int left, int top, int right, int bottom, boolean hovered) {
        context.fill(left, top, right, bottom, hovered ? ROW_BG_HOVER : ROW_BG);
    }

    public static void drawInputFrame(DrawContext context, int x, int y, int width, int height, boolean focused) {
        context.fill(x, y, x + width, y + height, INPUT_BG);
        int border = focused ? BUTTON_BORDER_HOVER : PANEL_BORDER;
        context.fill(x, y, x + width, y + 1, border);
        context.fill(x, y + height - 1, x + width, y + height, border);
        context.fill(x, y, x + 1, y + height, border);
        context.fill(x + width - 1, y, x + width, y + height, border);
    }

    public static void drawTitle(DrawContext context, TextRenderer textRenderer, Text title, int screenWidth) {
        drawTitle(context, textRenderer, title, screenWidth, 6);
    }

    public static void drawTitle(DrawContext context, TextRenderer textRenderer, Text title, int screenWidth, int y) {
        context.drawCenteredTextWithShadow(textRenderer, title, screenWidth / 2, y, TEXT_PRIMARY);
    }

    public static void drawScreenDim(DrawContext context, int screenWidth, int screenHeight) {
        context.fill(0, 0, screenWidth, screenHeight, OVERLAY_DIM);
    }

    public static int centeredTextY(int y, int height, int fontHeight) {
        return y + (height - fontHeight) / 2;
    }

    public static boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
