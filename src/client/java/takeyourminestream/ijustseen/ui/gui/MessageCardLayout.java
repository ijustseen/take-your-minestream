package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.messages.Message;
import takeyourminestream.ijustseen.messages.MessageEmote;

import java.util.List;

/** Расчёт компактного размера карточки сообщения для GUI. */
public final class MessageCardLayout {
    public static final int USERNAME_ROW_HEIGHT = 12;

    private MessageCardLayout() {}

    public static int getWrapWidth(int maxAvailableWidth) {
        return Math.min(
            MessagePanelConstants.MESSAGE_WRAP_WIDTH,
            Math.max(40, maxAvailableWidth - MessagePanelConstants.PADDING_X * 2)
        );
    }

    public record EmoteBodyLine(String text, List<MessageEmote> emotes) {}

    public record Layout(
        List<OrderedText> bodyLines,
        int width,
        int height,
        int contentWidth,
        EmoteBodyLine emoteBodyLine
    ) {
        public Layout(List<OrderedText> bodyLines, int width, int height, int contentWidth) {
            this(bodyLines, width, height, contentWidth, null);
        }
    }

    public static Layout computeHud(TextRenderer textRenderer, Message message, int maxAvailableWidth) {
        if (message.getEmotes() != null && !message.getEmotes().isEmpty()) {
            return computeWithEmotes(textRenderer, message, maxAvailableWidth);
        }
        return compute(textRenderer, message.getText(), maxAvailableWidth);
    }

    private static Layout computeWithEmotes(TextRenderer textRenderer, Message message, int maxAvailableWidth) {
        String rawMessageText = message.getText();
        ChatMessageParser.ParsedMessage parsed = ChatMessageParser.parse(rawMessageText);
        String bodyText = MessageEmoteGuiRenderer.getBodyText(rawMessageText);
        List<MessageEmote> bodyEmotes = MessageEmoteGuiRenderer.remapEmotesToBody(rawMessageText, message.getEmotes());

        int contentWidth = 0;
        if (parsed.username() != null) {
            contentWidth = textRenderer.getWidth(Text.empty().append(parsed.usernameText()));
        }
        int bodyWidth = MessageEmoteGuiRenderer.measureLine(textRenderer, bodyText, bodyEmotes);
        contentWidth = Math.max(contentWidth, bodyWidth);
        contentWidth = Math.max(contentWidth, 40);

        int cardWidth = Math.min(maxAvailableWidth, contentWidth + MessagePanelConstants.PADDING_X * 2);
        int cardHeight = MessagePanelConstants.PADDING_Y * 2;
        if (parsed.username() != null) {
            cardHeight += USERNAME_ROW_HEIGHT + 2;
        }
        cardHeight += textRenderer.fontHeight;

        return new Layout(
            List.of(),
            cardWidth,
            cardHeight,
            contentWidth,
            new EmoteBodyLine(bodyText, bodyEmotes)
        );
    }

    public static Layout compute(
        TextRenderer textRenderer,
        String rawMessageText,
        int maxAvailableWidth
    ) {
        ChatMessageParser.ParsedMessage parsed = ChatMessageParser.parse(rawMessageText);
        int wrapWidth = getWrapWidth(maxAvailableWidth);

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
