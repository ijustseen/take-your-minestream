package takeyourminestream.ijustseen.core.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** Парсит legacy-строки с кодами § (Minecraft 26.x / Mojang mappings). */
public final class LegacySectionText {
    private LegacySectionText() {}

    public static MutableComponent parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
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

    public static FormattedCharSequence toOrderedText(String text) {
        return parse(text).getVisualOrderText();
    }

    private static void flush(StringBuilder buffer, MutableComponent result, Style style) {
        if (!buffer.isEmpty()) {
            result.append(Component.literal(buffer.toString()).withStyle(style));
            buffer.setLength(0);
        }
    }

    private static Style applyCode(Style current, char code) {
        char lower = Character.toLowerCase(code);
        if (lower == 'r') {
            return Style.EMPTY;
        }

        ChatFormatting format = ChatFormatting.getByCode(lower);
        if (format == null) {
            return current;
        }

        if ((lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')) {
            return current.withColor(format);
        }

        return switch (format) {
            case OBFUSCATED -> current.withObfuscated(true);
            case BOLD -> current.withBold(true);
            case STRIKETHROUGH -> current.withStrikethrough(true);
            case UNDERLINE -> current.withUnderlined(true);
            case ITALIC -> current.withItalic(true);
            default -> current;
        };
    }
}
