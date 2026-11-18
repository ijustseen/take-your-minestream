package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.messages.TwitchEmoteTextureCache;

/**
 * Баннер источника: фирменная заливка, крупная иконка и название.
 * Один и тот же вид на карточках главной страницы и в шапке страницы источника.
 */
public final class PlatformBanner {
    private static final int ICON_PADDING = 10;
    private static final int MAX_ICON_SIZE = 22;
    private static final int ICON_TO_TEXT_GAP = 4;

    private PlatformBanner() {}

    public static void draw(
        DrawContext context,
        TextRenderer textRenderer,
        ChatPlatform platform,
        int x,
        int y,
        int width,
        int height,
        boolean hovered
    ) {
        int accent = platform.getAccentColorRgb();
        int bottom = y + height;
        context.fill(x, y, x + width, bottom, (hovered ? 0x66000000 : 0x40000000) | accent);
        context.fill(x, y, x + width, y + 1, 0xFF000000 | accent);
        context.fill(x, bottom - 2, x + width, bottom, 0xFF000000 | accent);

        int iconSize = Math.min(MAX_ICON_SIZE, height - ICON_PADDING);
        int iconX = x + ICON_PADDING;
        int textX = iconX;
        Identifier icon = TwitchEmoteTextureCache.getTextureIdentifier("platform", platform.getIconKey());
        if (icon != null) {
            textX = MessageEmoteGuiRenderer.drawGuiIcon(
                context,
                icon,
                iconX,
                y + (height - iconSize) / 2,
                iconSize
            ) + ICON_TO_TEXT_GAP;
        }
        context.drawTextWithShadow(
            textRenderer,
            Text.literal(platform.getDisplayName()),
            textX,
            ModUiTheme.centeredTextY(y, height, textRenderer.fontHeight),
            ModUiTheme.TEXT_PRIMARY
        );
    }
}
