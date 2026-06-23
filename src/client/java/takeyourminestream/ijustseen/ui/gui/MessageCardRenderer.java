package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.messages.EmoteTextLayout;
import takeyourminestream.ijustseen.messages.Message;

/** Отрисовка одной карточки сообщения в GUI-истории. */
public final class MessageCardRenderer {
    public record UsernameHitbox(int x, int y, int width, int height) {}

    public record DrawResult(UsernameHitbox usernameHitbox) {}

    private MessageCardRenderer() {}

    public static DrawResult draw(
        DrawContext context,
        TextRenderer textRenderer,
        BlockedUsernameManager blockedUsernameManager,
        Message message,
        ChatMessageParser.ParsedMessage parsed,
        MessageCardLayout.Layout layout,
        int x,
        int y,
        float alpha,
        boolean hovered,
        boolean visibleInWorld,
        boolean pinnedInWorld
    ) {
        float panelAlpha = visibleInWorld ? alpha : alpha * 0.55f;
        MessagePanelGuiRenderer.drawPanel(context, x, y, layout.width(), layout.height(), panelAlpha, borderColorFor(message));

        int innerX = x + MessagePanelConstants.PADDING_X;
        int innerY = y + MessagePanelConstants.PADDING_Y;
        UsernameHitbox usernameHitbox = null;

        if (parsed.username() != null) {
            boolean blocked = blockedUsernameManager.isBlocked(parsed.username(), parsed.username());
            MutableText usernameLabel = Text.empty().append(parsed.usernameText());
            if (blocked) {
                usernameLabel.formatted(Formatting.DARK_RED, Formatting.STRIKETHROUGH);
            } else if (hovered) {
                usernameLabel.formatted(Formatting.YELLOW, Formatting.UNDERLINE);
            }

            int usernameX = innerX;
            String platformIconKey = message.getPlatformIconKey();
            if (platformIconKey != null) {
                usernameX = MessageEmoteGuiRenderer.drawPlatformIcon(context, platformIconKey, innerX, innerY);
            }

            context.drawTextWithShadow(textRenderer, usernameLabel, usernameX, innerY, visibleInWorld ? 0xFFFFFFFF : 0xFF888888);
            int usernameWidth = textRenderer.getWidth(usernameLabel);
            usernameHitbox = new UsernameHitbox(usernameX, innerY, usernameWidth, MessageCardLayout.USERNAME_ROW_HEIGHT);

            if (blocked) {
                Text blockedBadge = Text.translatable("takeyourstreamchat.history.blocked_badge").formatted(Formatting.RED);
                context.drawTextWithShadow(
                    textRenderer,
                    blockedBadge,
                    usernameX + usernameWidth + 4,
                    innerY,
                    0xFFFF5555
                );
            }

            innerY += MessageCardLayout.USERNAME_ROW_HEIGHT + 2;
        }

        int bodyColor = applyAlpha(visibleInWorld ? (hovered ? 0xFFFFFFAA : 0xFFFFFFFF) : 0xFF888888, alpha);
        if (layout.emoteBodyLine() != null) {
            int lineY = innerY;
            for (EmoteTextLayout.LineContent line : layout.emoteBodyLine().lines()) {
                MessageEmoteGuiRenderer.drawLine(
                    context,
                    textRenderer,
                    line.text(),
                    line.emotes(),
                    innerX,
                    lineY,
                    bodyColor
                );
                lineY += MessageCardLayout.emoteLineAdvance(textRenderer, line);
            }
        } else {
            for (OrderedText line : layout.bodyLines()) {
                context.drawTextWithShadow(textRenderer, line, innerX, innerY, bodyColor);
                innerY += textRenderer.fontHeight;
            }
        }

        if (pinnedInWorld) {
            MessagePanelGuiRenderer.drawPinIcon(context, x, y, layout.width(), panelAlpha);
        }

        return new DrawResult(usernameHitbox);
    }

    /** Карточка для HUD-оверлея (без hover-эффектов истории). */
    public static void drawHudCard(
        DrawContext context,
        TextRenderer textRenderer,
        BlockedUsernameManager blockedUsernameManager,
        Message message,
        ChatMessageParser.ParsedMessage parsed,
        MessageCardLayout.Layout layout,
        int x,
        int y,
        float alpha
    ) {
        if (ModConfig.isSHOW_MESSAGE_BACKGROUND()) {
            MessagePanelGuiRenderer.drawPanel(context, x, y, layout.width(), layout.height(), alpha, borderColorFor(message));
        }

        int innerX = x + MessagePanelConstants.PADDING_X;
        int innerY = y + MessagePanelConstants.PADDING_Y;

        if (parsed.username() != null) {
            boolean blocked = blockedUsernameManager.isBlocked(parsed.username(), parsed.username());
            MutableText usernameLabel = Text.empty().append(parsed.usernameText());
            if (blocked) {
                usernameLabel.formatted(Formatting.DARK_RED, Formatting.STRIKETHROUGH);
            }
            int usernameX = innerX;
            String platformIconKey = message.getPlatformIconKey();
            if (platformIconKey != null) {
                usernameX = MessageEmoteGuiRenderer.drawPlatformIcon(context, platformIconKey, innerX, innerY);
            }
            int usernameColor = usernameColor(message, alpha);
            context.drawTextWithShadow(textRenderer, usernameLabel, usernameX, innerY, usernameColor);
            if (blocked) {
                Text blockedBadge = Text.translatable("takeyourstreamchat.history.blocked_badge").formatted(Formatting.RED);
                context.drawTextWithShadow(
                    textRenderer,
                    blockedBadge,
                    usernameX + textRenderer.getWidth(usernameLabel) + 4,
                    innerY,
                    applyAlpha(0xFFFF5555, alpha)
                );
            }
            innerY += MessageCardLayout.USERNAME_ROW_HEIGHT + 2;
        }

        int bodyColor = applyAlpha(0xFFFFFFFF, alpha);
        if (layout.emoteBodyLine() != null) {
            int lineY = innerY;
            for (EmoteTextLayout.LineContent line : layout.emoteBodyLine().lines()) {
                MessageEmoteGuiRenderer.drawLine(
                    context,
                    textRenderer,
                    line.text(),
                    line.emotes(),
                    innerX,
                    lineY,
                    bodyColor
                );
                lineY += MessageCardLayout.emoteLineAdvance(textRenderer, line);
            }
        } else {
            for (OrderedText line : layout.bodyLines()) {
                context.drawTextWithShadow(textRenderer, line, innerX, innerY, bodyColor);
                innerY += textRenderer.fontHeight;
            }
        }
    }

    private static int usernameColor(Message message, float alpha) {
        Integer rgb = message.getAuthorColorRgb();
        if (rgb != null) {
            return applyAlpha(0xFF000000 | (rgb & 0xFFFFFF), alpha);
        }
        return applyAlpha(0xFFFFFFFF, alpha);
    }

    private static int borderColorFor(Message message) {
        return takeyourminestream.ijustseen.integration.chat.ChatPlatform
            .accentColorForIconKey(message.getPlatformIconKey());
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) {
            a = 255;
        }
        return (color & 0x00FFFFFF) | (((int) (a * alpha)) << 24);
    }
}
