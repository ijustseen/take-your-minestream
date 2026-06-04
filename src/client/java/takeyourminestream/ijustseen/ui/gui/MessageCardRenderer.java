package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
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
        MessagePanelGuiRenderer.drawPanel(context, x, y, layout.width(), layout.height(), panelAlpha);

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

            context.drawText(textRenderer, usernameLabel, innerX, innerY, visibleInWorld ? 0xFFFFFFFF : 0xFF888888, false);
            int usernameWidth = textRenderer.getWidth(usernameLabel);
            usernameHitbox = new UsernameHitbox(innerX, innerY, usernameWidth, MessageCardLayout.USERNAME_ROW_HEIGHT);

            if (blocked) {
                Text blockedBadge = Text.translatable("takeyourminestream.history.blocked_badge").formatted(Formatting.RED);
                context.drawText(
                    textRenderer,
                    blockedBadge,
                    innerX + usernameWidth + 4,
                    innerY,
                    0xFFFF5555,
                    false
                );
            }

            innerY += MessageCardLayout.USERNAME_ROW_HEIGHT + 2;
        }

        int bodyColor = applyAlpha(visibleInWorld ? (hovered ? 0xFFFFFFAA : 0xFFFFFFFF) : 0xFF888888, alpha);
        for (OrderedText line : layout.bodyLines()) {
            context.drawText(textRenderer, line, innerX, innerY, bodyColor, false);
            innerY += textRenderer.fontHeight;
        }

        if (pinnedInWorld) {
            MessagePanelGuiRenderer.drawPinIcon(context, x, y, layout.width(), panelAlpha);
        }

        return new DrawResult(usernameHitbox);
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) {
            a = 255;
        }
        return (color & 0x00FFFFFF) | (((int) (a * alpha)) << 24);
    }
}
