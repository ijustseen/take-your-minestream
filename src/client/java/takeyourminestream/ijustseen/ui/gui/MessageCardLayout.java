package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;

import java.util.List;

/** Расчёт компактного размера карточки сообщения для GUI. */
public final class MessageCardLayout {
    public static final int USERNAME_ROW_HEIGHT = 12;
    public static final int MAX_WRAP_WIDTH = 420;

    private MessageCardLayout() {}

    public record Layout(
        List<OrderedText> bodyLines,
        int width,
        int height,
        int contentWidth
    ) {}

    public static Layout compute(
        TextRenderer textRenderer,
        String rawMessageText,
        int maxAvailableWidth
    ) {
        ChatMessageParser.ParsedMessage parsed = ChatMessageParser.parse(rawMessageText);
        int wrapWidth = Math.min(MAX_WRAP_WIDTH, maxAvailableWidth - MessagePanelConstants.PADDING_X * 2);

        List<OrderedText> bodyLines = parsed.bodyPlain().isBlank()
            ? List.of()
            : textRenderer.wrapLines(parsed.bodyText(), wrapWidth);

        if (bodyLines.isEmpty() && parsed.username() == null) {
            bodyLines = textRenderer.wrapLines(Text.literal(rawMessageText), wrapWidth);
        }

        int contentWidth = 0;
        if (parsed.username() != null) {
            contentWidth = textRenderer.getWidth(Text.empty().append(parsed.usernameText()));
        }
        for (OrderedText line : bodyLines) {
            contentWidth = Math.max(contentWidth, textRenderer.getWidth(line));
        }

        contentWidth = Math.max(contentWidth, 40);
        int cardWidth = Math.min(maxAvailableWidth, contentWidth + MessagePanelConstants.PADDING_X * 2);

        int cardHeight = MessagePanelConstants.PADDING_Y * 2;
        if (parsed.username() != null) {
            cardHeight += USERNAME_ROW_HEIGHT + 2;
        }
        if (!bodyLines.isEmpty()) {
            cardHeight += bodyLines.size() * textRenderer.fontHeight;
        }

        return new Layout(bodyLines, cardWidth, cardHeight, contentWidth);
    }
}
