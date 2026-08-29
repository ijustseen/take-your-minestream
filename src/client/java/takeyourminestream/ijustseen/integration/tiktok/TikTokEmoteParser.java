package takeyourminestream.ijustseen.integration.tiktok;

import takeyourminestream.ijustseen.integration.tiktok.proto.TikTokProto;
import takeyourminestream.ijustseen.messages.MessageEmote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inline-эмодзи TikTok из protobuf (подписчиковые стикеры с URL на CDN).
 * <p>
 * TODO: LIVE-шорткоды вроде {@code [hi]} / {@code [smile]} приходят обычным текстом,
 * без картинки. В клиенте TikTok это встроенные {@code data:image} ассеты, публичного
 * CDN/библиотеки с тем же набором нет — чужие PNG (Emojipedia, Douyin list) не совпадают.
 * Пока не рисуем их вообще, чтобы не подменять чат неверными картинками.
 */
final class TikTokEmoteParser {
    static final String PROVIDER = "tiktok";
    /** Поля {@code repeated EmoteWithIndex} в {@code WebcastChatMessage} (обычно 13). */
    private static final int[] EMOTE_LIST_FIELDS = {13, 14, 15, 16, 17, 18};

    private TikTokEmoteParser() {}

    static List<MessageEmote> parseChatEmotes(TikTokProto.ProtoMap chat, String comment) {
        return parseProtoEmotes(chat, comment);
    }

    static MessageEmote parseStandaloneEmote(TikTokProto.ProtoMap emoteDetails) {
        String cacheKey = resolveCacheKey(emoteDetails);
        if (cacheKey == null) {
            return null;
        }
        // Один символ-заглушка под inline-эмодзи (Object Replacement Character).
        return toEmote(cacheKey, "\uFFFC", 0, 0);
    }

    static String standaloneEmoteText() {
        return "\uFFFC";
    }

    private static List<MessageEmote> parseProtoEmotes(TikTokProto.ProtoMap chat, String comment) {
        if (chat == null || comment == null || comment.isEmpty()) {
            return List.of();
        }
        List<SubEmoteEntry> entries = new ArrayList<>();
        for (int field : EMOTE_LIST_FIELDS) {
            for (TikTokProto.ProtoMap subEmote : chat.getRepeatedMessages(field)) {
                int place = subEmote.getInt(1);
                String cacheKey = resolveCacheKey(subEmote.getMessage(2));
                if (cacheKey == null) {
                    continue;
                }
                entries.add(new SubEmoteEntry(place, cacheKey));
            }
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
            emotes.add(toEmote(entry.cacheKey, emoteCode, range[0], range[1]));
        }
        return emotes.isEmpty() ? List.of() : List.copyOf(emotes);
    }

    private static MessageEmote toEmote(String cacheKey, String code, int start, int end) {
        return new MessageEmote(PROVIDER, cacheKey, code, start, end);
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
        if (emoteId.isBlank()) {
            return null;
        }
        if (emoteId.startsWith("http://") || emoteId.startsWith("https://")) {
            return emoteId;
        }
        return emoteId;
    }

    private static String extractImageUrl(TikTokProto.ProtoMap image) {
        if (image == null) {
            return null;
        }
        String direct = firstHttpUrl(image.getString(1));
        if (direct != null) {
            return direct;
        }
        for (String url : image.getRepeatedStrings(1)) {
            String http = firstHttpUrl(url);
            if (http != null) {
                return http;
            }
        }
        String uri = image.getString(2);
        String fromUri = firstHttpUrl(uri);
        if (fromUri != null) {
            return fromUri;
        }
        if (uri != null && !uri.isBlank() && !uri.contains(" ")) {
            return "https://p16-webcast.tiktokcdn.com/img/" + uri + "~tplv-obj.png";
        }
        return null;
    }

    private static String firstHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return null;
    }

    /** Диапазон символов плейсхолдера эмодзи в тексте комментария. */
    private static int[] emoteRange(String text, int start) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (start > 0 && start <= text.length() && text.charAt(start - 1) == '[') {
            start = start - 1;
        }
        if (start < 0 || start >= text.length()) {
            return null;
        }
        if (text.charAt(start) == '[') {
            int end = text.indexOf(']', start);
            if (end > start && end < text.length()) {
                return new int[] {start, end};
            }
        }
        int open = text.lastIndexOf('[', start);
        int close = text.indexOf(']', start);
        if (open >= 0 && close > open) {
            return new int[] {open, close};
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
