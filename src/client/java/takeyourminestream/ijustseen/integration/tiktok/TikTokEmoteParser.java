package takeyourminestream.ijustseen.integration.tiktok;

import takeyourminestream.ijustseen.integration.tiktok.proto.TikTokProto;
import takeyourminestream.ijustseen.messages.MessageEmote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Парсинг inline-эмодзи TikTok из {@code WebcastChatMessage} / {@code EmoteDetails}. */
final class TikTokEmoteParser {
    private static final String PROVIDER = "tiktok";

    private TikTokEmoteParser() {}

    static List<MessageEmote> parseChatEmotes(TikTokProto.ProtoMap chat, String comment) {
        List<TikTokProto.ProtoMap> subEmotes = chat.getRepeatedMessages(13);
        if (subEmotes.isEmpty() || comment == null || comment.isEmpty()) {
            return List.of();
        }

        List<SubEmoteEntry> entries = new ArrayList<>();
        for (TikTokProto.ProtoMap subEmote : subEmotes) {
            int place = subEmote.getInt(1);
            TikTokProto.ProtoMap details = subEmote.getMessage(2);
            String cacheKey = resolveCacheKey(details);
            if (cacheKey == null) {
                continue;
            }
            entries.add(new SubEmoteEntry(place, cacheKey));
        }

        if (entries.isEmpty()) {
            return List.of();
        }

        entries.sort(Comparator.comparingInt(SubEmoteEntry::place));
        List<MessageEmote> emotes = new ArrayList<>();
        for (SubEmoteEntry entry : entries) {
            int[] range = emoteRange(comment, entry.place);
            if (range == null) {
                continue;
            }
            String emoteCode = comment.substring(range[0], range[1] + 1);
            emotes.add(new MessageEmote(PROVIDER, entry.cacheKey, emoteCode, range[0], range[1]));
        }
        return emotes.isEmpty() ? List.of() : List.copyOf(emotes);
    }

    static MessageEmote parseStandaloneEmote(TikTokProto.ProtoMap emoteDetails) {
        String cacheKey = resolveCacheKey(emoteDetails);
        if (cacheKey == null) {
            return null;
        }
        // Один символ-заглушка под inline-эмодзи (Object Replacement Character).
        return new MessageEmote(PROVIDER, cacheKey, "\uFFFC", 0, 0);
    }

    static String standaloneEmoteText() {
        return "\uFFFC";
    }

    private static String resolveCacheKey(TikTokProto.ProtoMap details) {
        if (details == null) {
            return null;
        }
        String imageUrl = extractImageUrl(details.getMessage(2));
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        String emoteId = details.getString(1);
        return emoteId.isBlank() ? null : emoteId;
    }

    private static String extractImageUrl(TikTokProto.ProtoMap image) {
        if (image == null) {
            return null;
        }
        String direct = image.getString(1);
        if (!direct.isBlank() && direct.startsWith("http")) {
            return direct;
        }
        for (String url : image.getRepeatedStrings(1)) {
            if (url != null && !url.isBlank() && url.startsWith("http")) {
                return url;
            }
        }
        return direct.isBlank() ? null : direct;
    }

    /** Диапазон символов плейсхолдера эмодзи в тексте комментария. */
    private static int[] emoteRange(String text, int start) {
        if (start < 0 || start >= text.length()) {
            return null;
        }
        if (text.charAt(start) == '[') {
            int end = text.indexOf(']', start);
            if (end > start && end < text.length()) {
                return new int[] {start, end};
            }
        }
        int codePoint = text.codePointAt(start);
        int end = start + Character.charCount(codePoint) - 1;
        if (end >= text.length()) {
            return null;
        }
        return new int[] {start, end};
    }

    private record SubEmoteEntry(int place, String cacheKey) {}
}
