package takeyourminestream.ijustseen.core.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Formatting;

/**
 * Парсит legacy-строки с кодами § (как в Twitch-чате) в компоненты Minecraft.
 */
public final class LegacySectionText {
    private LegacySectionText() {}

    public static MutableText parse(String text) {
        if (text == null || text.isEmpty()) {
            return Text.empty();
        }

        MutableText result = Text.empty();
        Style style = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                flush(buffer, result, style);
                style = applyCode(style, text.charAt(i + 1));
                i++;
            } else {
                buffer.append(c);
            }
        }

        flush(buffer, result, style);
        return result;
    }

    public static OrderedText toOrderedText(String text) {
        return parse(text).asOrderedText();
    }

    private static void flush(StringBuilder buffer, MutableText result, Style style) {
        if (!buffer.isEmpty()) {
            result.append(Text.literal(buffer.toString()).styled(s -> style));
            buffer.setLength(0);
        }
    }

    private static Style applyCode(Style current, char code) {
        char lower = Character.toLowerCase(code);
        if (lower == 'r') {
            return Style.EMPTY;
        }

        Formatting format = Formatting.byCode(lower);
        if (format == null) {
            return current;
        }

        if (format.isColor()) {
            return current.withColor(format);
        }

        return switch (format) {
            case OBFUSCATED -> current.withObfuscated(true);
            case BOLD -> current.withBold(true);
            case STRIKETHROUGH -> current.withStrikethrough(true);
            case UNDERLINE -> current.withUnderline(true);
            case ITALIC -> current.withItalic(true);
            default -> current;
        };
    }
}
