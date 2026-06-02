package takeyourminestream.ijustseen.core.text;

import net.minecraft.text.Text;

/** Парсинг формата Twitch-сообщений: {@code §aUsername:§r текст}. */
public final class ChatMessageParser {
    private static final String SEPARATOR = ":§r ";

    private ChatMessageParser() {}

    public static ParsedMessage parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ParsedMessage(null, Text.empty(), Text.empty(), "");
        }

        int separatorIndex = rawText.indexOf(SEPARATOR);
        if (separatorIndex >= 0) {
            String usernamePart = rawText.substring(0, separatorIndex);
            String bodyPart = rawText.substring(separatorIndex + SEPARATOR.length());
            String username = stripFormatting(usernamePart);
            return new ParsedMessage(
                username.isBlank() ? null : username,
                Text.literal(usernamePart + ":"),
                Text.literal(bodyPart),
                bodyPart
            );
        }

        return new ParsedMessage(null, Text.empty(), Text.literal(rawText), rawText);
    }

    public static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            result.append(c);
        }
        return result.toString().trim();
    }

    public record ParsedMessage(String username, Text usernameText, Text bodyText, String bodyPlain) {}
}
