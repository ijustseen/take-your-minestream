package takeyourminestream.ijustseen.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Перенос строк для текста с инлайн-эмоутами.
 * Возвращает строки, где индексы эмоутов пересчитаны относительно текста строки.
 */
public final class EmoteTextLayout {
    private EmoteTextLayout() {}

    /** Одна строка после переноса: текст и эмоуты с индексами относительно этого текста. */
    public record LineContent(String text, List<MessageEmote> emotes) {
        public boolean hasEmotes() {
            return !emotes.isEmpty();
        }
    }

    private sealed interface Token permits TextToken, EmoteToken {}

    private record TextToken(String text, boolean isSpace) implements Token {}

    private record EmoteToken(MessageEmote emote, String sourceText) implements Token {}

    /**
     * Разбивает текст с эмоутами на строки шириной не более {@code maxWidth}.
     *
     * @param measure         ширина текстового фрагмента в пикселях
     * @param text            полный текст сообщения
     * @param emotes          эмоуты с индексами в {@code text} (невалидные диапазоны игнорируются)
     * @param emoteAdvance    занимаемая эмоутом ширина (иконка + отступ)
     * @param maxWidth        максимальная ширина строки
     * @param firstLineIndent зарезервированная ширина в начале первой строки (например, иконка платформы)
     */
    public static List<LineContent> wrap(
        ToIntFunction<String> measure,
        String text,
        List<MessageEmote> emotes,
        int emoteAdvance,
        int maxWidth,
        int firstLineIndent
    ) {
        List<Token> tokens = tokenize(text, emotes);
        List<LineContent> lines = new ArrayList<>();

        StringBuilder lineText = new StringBuilder();
        List<MessageEmote> lineEmotes = new ArrayList<>();
        int lineWidth = firstLineIndent;
        boolean lineHasContent = false;
        String activeFormatting = "";

        for (Token token : tokens) {
            if (token instanceof EmoteToken emoteToken) {
                if (lineHasContent && lineWidth + emoteAdvance > maxWidth) {
                    lines.add(new LineContent(lineText.toString(), List.copyOf(lineEmotes)));
                    lineText = new StringBuilder(activeFormatting);
                    lineEmotes = new ArrayList<>();
                    lineWidth = 0;
                    lineHasContent = false;
                }
                int start = lineText.length();
                lineText.append(emoteToken.sourceText());
                lineEmotes.add(new MessageEmote(
                    emoteToken.emote().getProvider(),
                    emoteToken.emote().getEmoteId(),
                    emoteToken.emote().getEmoteCode(),
                    start,
                    lineText.length() - 1
                ));
                lineWidth += emoteAdvance;
                lineHasContent = true;
                continue;
            }

            TextToken textToken = (TextToken) token;
            String word = textToken.text();
            int wordWidth = measure.applyAsInt(word);

            if (lineHasContent && lineWidth + wordWidth > maxWidth) {
                lines.add(new LineContent(lineText.toString(), List.copyOf(lineEmotes)));
                lineText = new StringBuilder(activeFormatting);
                lineEmotes = new ArrayList<>();
                lineWidth = 0;
                lineHasContent = false;
                if (textToken.isSpace()) {
                    activeFormatting = updateFormatting(activeFormatting, word);
                    continue;
                }
            }

            if (!textToken.isSpace() && wordWidth > maxWidth) {
                // Слово целиком не помещается — режем по символам
                for (int i = 0; i < word.length(); i++) {
                    char c = word.charAt(i);
                    String chunk = String.valueOf(c);
                    int chunkWidth = measure.applyAsInt(chunk);
                    if (lineHasContent && lineWidth + chunkWidth > maxWidth) {
                        lines.add(new LineContent(lineText.toString(), List.copyOf(lineEmotes)));
                        lineText = new StringBuilder(activeFormatting);
                        lineEmotes = new ArrayList<>();
                        lineWidth = 0;
                        lineHasContent = false;
                    }
                    lineText.append(c);
                    lineWidth += chunkWidth;
                    lineHasContent = true;
                }
            } else {
                lineText.append(word);
                lineWidth += wordWidth;
                if (!textToken.isSpace()) {
                    lineHasContent = true;
                }
            }
            activeFormatting = updateFormatting(activeFormatting, word);
        }

        if (lineText.length() > 0 || !lineEmotes.isEmpty()) {
            lines.add(new LineContent(lineText.toString(), List.copyOf(lineEmotes)));
        }
        if (lines.isEmpty()) {
            lines.add(new LineContent(text, List.of()));
        }
        return lines;
    }

    /** Текущие активные коды форматирования (§x), которые надо перенести на следующую строку. */
    private static String updateFormatting(String current, String appended) {
        String result = current;
        for (int i = 0; i < appended.length() - 1; i++) {
            if (appended.charAt(i) != '§') {
                continue;
            }
            char code = Character.toLowerCase(appended.charAt(i + 1));
            if (code == 'r') {
                result = "";
            } else if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                result = "§" + code;
            } else if (code == 'k' || code == 'l' || code == 'm' || code == 'n' || code == 'o') {
                result = result + "§" + code;
            }
            i++;
        }
        return result;
    }

    private static List<Token> tokenize(String text, List<MessageEmote> emotes) {
        List<Token> tokens = new ArrayList<>();
        int cursor = 0;
        for (MessageEmote emote : sortedValidEmotes(text, emotes)) {
            if (emote.getStartIndex() > cursor) {
                addTextTokens(tokens, text.substring(cursor, emote.getStartIndex()));
            }
            tokens.add(new EmoteToken(emote, text.substring(emote.getStartIndex(), emote.getEndIndex() + 1)));
            cursor = emote.getEndIndex() + 1;
        }
        if (cursor < text.length()) {
            addTextTokens(tokens, text.substring(cursor));
        }
        return tokens;
    }

    /** Разбивает текст на слова и пробелы (пробелы — отдельные токены). */
    private static void addTextTokens(List<Token> tokens, String text) {
        int start = 0;
        while (start < text.length()) {
            boolean isSpace = text.charAt(start) == ' ';
            int end = start;
            while (end < text.length() && (text.charAt(end) == ' ') == isSpace) {
                end++;
            }
            tokens.add(new TextToken(text.substring(start, end), isSpace));
            start = end;
        }
    }

    private static List<MessageEmote> sortedValidEmotes(String text, List<MessageEmote> emotes) {
        if (emotes == null || emotes.isEmpty()) {
            return Collections.emptyList();
        }
        List<MessageEmote> sorted = new ArrayList<>(emotes);
        sorted.sort(Comparator.comparingInt(MessageEmote::getStartIndex));

        List<MessageEmote> valid = new ArrayList<>();
        int nextAllowedStart = 0;
        for (MessageEmote emote : sorted) {
            if (emote.getStartIndex() < 0 || emote.getEndIndex() < emote.getStartIndex() || emote.getEndIndex() >= text.length()) {
                continue;
            }
            if (emote.getStartIndex() < nextAllowedStart) {
                continue;
            }
            valid.add(emote);
            nextAllowedStart = emote.getEndIndex() + 1;
        }
        return valid;
    }
}
