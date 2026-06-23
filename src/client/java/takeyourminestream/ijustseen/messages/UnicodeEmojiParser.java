package takeyourminestream.ijustseen.messages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Находит Unicode-эмодзи в тексте и строит {@link MessageEmote} для inline-отрисовки. */
public final class UnicodeEmojiParser {
    private UnicodeEmojiParser() {}

    /**
     * @param text     полный текст сообщения
     * @param occupied уже занятые диапазоны (Twitch/7TV/TikTok эмоуты)
     */
    public static List<MessageEmote> parse(String text, List<MessageEmote> occupied) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<MessageEmote> result = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            if (isBlocked(index, index, occupied)) {
                index = nextUnblocked(index, occupied, text.length());
                continue;
            }

            int clusterEnd = endOfEmojiCluster(text, index);
            if (clusterEnd <= index) {
                index += Character.charCount(text.codePointAt(index));
                continue;
            }

            if (isBlocked(index, clusterEnd - 1, occupied)) {
                index = clusterEnd;
                continue;
            }

            String sequence = text.substring(index, clusterEnd);
            String cacheId = EmojiTextureCache.cacheIdFor(sequence);
            result.add(new MessageEmote("emoji", cacheId, sequence, index, clusterEnd - 1));
            index = clusterEnd;
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    public static List<MessageEmote> mergeNonOverlapping(List<MessageEmote> base, List<MessageEmote> extra) {
        if (extra == null || extra.isEmpty()) {
            return base == null ? List.of() : base;
        }
        if (base == null || base.isEmpty()) {
            return List.copyOf(extra);
        }

        List<MessageEmote> merged = new ArrayList<>(base.size() + extra.size());
        merged.addAll(base);
        merged.addAll(extra);
        merged.sort(Comparator.comparingInt(MessageEmote::getStartIndex));

        List<MessageEmote> valid = new ArrayList<>();
        int nextAllowedStart = 0;
        for (MessageEmote emote : merged) {
            if (emote.getStartIndex() < 0) {
                valid.add(emote);
                continue;
            }
            if (emote.getStartIndex() < nextAllowedStart) {
                continue;
            }
            valid.add(emote);
            nextAllowedStart = emote.getEndIndex() + 1;
        }
        return List.copyOf(valid);
    }

    private static int endOfEmojiCluster(String text, int start) {
        int cp = text.codePointAt(start);
        int end = start + Character.charCount(cp);

        if (isKeycapBase(cp)) {
            int cursor = end;
            if (cursor < text.length() && text.codePointAt(cursor) == 0xFE0F) {
                cursor += Character.charCount(0xFE0F);
            }
            if (cursor < text.length() && text.codePointAt(cursor) == 0x20E3) {
                return cursor + Character.charCount(0x20E3);
            }
            return start;
        }

        if (isRegionalIndicator(cp)) {
            if (end < text.length()) {
                int cp2 = text.codePointAt(end);
                if (isRegionalIndicator(cp2)) {
                    return end + Character.charCount(cp2);
                }
            }
            return start;
        }

        if (!isEmojiStarter(cp)) {
            return start;
        }

        while (end < text.length()) {
            int next = text.codePointAt(end);
            if (next == 0x200D) {
                end += Character.charCount(next);
                if (end >= text.length()) {
                    break;
                }
                int afterZwj = text.codePointAt(end);
                if (!isEmojiStarter(afterZwj)) {
                    break;
                }
                end += Character.charCount(afterZwj);
                continue;
            }
            if (next == 0xFE0F || isSkinToneModifier(next)) {
                end += Character.charCount(next);
                continue;
            }
            break;
        }
        return end;
    }

    private static boolean isEmojiStarter(int codePoint) {
        return Character.isExtendedPictographic(codePoint);
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    private static boolean isSkinToneModifier(int codePoint) {
        return codePoint >= 0x1F3FB && codePoint <= 0x1F3FF;
    }

    private static boolean isKeycapBase(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9') || codePoint == '#' || codePoint == '*';
    }

    private static boolean isBlocked(int start, int end, List<MessageEmote> occupied) {
        if (occupied == null || occupied.isEmpty()) {
            return false;
        }
        for (MessageEmote emote : occupied) {
            if (emote.getStartIndex() < 0) {
                continue;
            }
            if (start <= emote.getEndIndex() && end >= emote.getStartIndex()) {
                return true;
            }
        }
        return false;
    }

    private static int nextUnblocked(int index, List<MessageEmote> occupied, int textLength) {
        int cursor = index + 1;
        while (cursor < textLength) {
            if (!isBlocked(cursor, cursor, occupied)) {
                return cursor;
            }
            cursor++;
        }
        return textLength;
    }
}
