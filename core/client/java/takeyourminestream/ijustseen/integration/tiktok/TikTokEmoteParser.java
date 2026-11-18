package takeyourminestream.ijustseen.integration.tiktok;

import takeyourminestream.ijustseen.integration.tiktok.proto.TikTokProto;
import takeyourminestream.ijustseen.messages.MessageEmote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Картинки TikTok только с настоящего CDN-адреса из protobuf.
 * Голый id и угаданный URL не используем: загрузчик принял бы их за эмоут Twitch
 * или скачал JSON-ошибку и показал бы её как текстуру.
 * Шорткод {@code [smile]} без картинки в сообщении остаётся текстом: веб-клиент
 * подменяет такие коды своим списком, а в эфир этот список не приходит.
 */
final class TikTokEmoteParser {
    static final String PROVIDER = "tiktok";
    /** {@code repeated EmoteWithIndex emotes = 13} в {@code WebcastChatMessage}. */
    private static final int CHAT_EMOTE_FIELD = 13;
    /** {@code repeated EmoteModel emote_list = 3} в {@code WebcastEmoteChatMessage}. */
    private static final int STICKER_LIST_FIELD = 3;

    private TikTokEmoteParser() {}

    static List<MessageEmote> parseChatEmotes(TikTokProto.ProtoMap chat, String comment) {
        return parseProtoEmotes(chat, comment);
    }

    /** Комментарий без текста, но с картинками в поле emotes. */
    static Stickers imagesOnly(TikTokProto.ProtoMap chat) {
        if (chat == null) {
            return null;
        }
        return stickersFrom(chat.getRepeatedMessages(CHAT_EMOTE_FIELD), true);
    }

    /** Стикеры подписчика: каждая картинка на своей строке, крупно. */
    static Stickers parseStickers(TikTokProto.ProtoMap chat) {
        if (chat == null) {
            return null;
        }
        List<TikTokProto.ProtoMap> models = new ArrayList<>(chat.getRepeatedMessages(STICKER_LIST_FIELD));
        if (models.isEmpty()) {
            TikTokProto.ProtoMap single = chat.getMessage(STICKER_LIST_FIELD);
            if (imageUrl(single) != null) {
                models.add(single);
            }
        }
        return stickersFrom(models, false);
    }

    private static Stickers stickersFrom(List<TikTokProto.ProtoMap> models, boolean nestedEmote) {
        StringBuilder text = new StringBuilder();
        List<MessageEmote> emotes = new ArrayList<>();
        for (TikTokProto.ProtoMap model : models) {
            String url = imageUrl(nestedEmote ? model.getMessage(2) : model);
            if (url == null && nestedEmote) {
                url = imageUrl(model);
            }
            if (url == null) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append(' ');
            }
            String token = "[sticker]";
            int start = text.length();
            text.append(token);
            emotes.add(new MessageEmote(
                MessageEmote.GIF_PROVIDER,
                url,
                token,
                start,
                start + token.length() - 1
            ));
        }
        if (emotes.isEmpty()) {
            return null;
        }
        return new Stickers(text.toString(), List.copyOf(emotes));
    }

    /** Иконка подарка (поле image, иначе icon). Индексы проставит вызывающий код. */
    static String giftImageUrl(TikTokProto.ProtoMap gift) {
        if (gift == null) {
            return null;
        }
        String image = extractImageUrl(gift.getMessage(1));
        if (image != null) {
            return image;
        }
        return extractImageUrl(gift.getMessage(21));
    }

    private static List<MessageEmote> parseProtoEmotes(TikTokProto.ProtoMap chat, String comment) {
        if (chat == null || comment == null || comment.isEmpty()) {
            return List.of();
        }
        List<SubEmoteEntry> entries = new ArrayList<>();
        for (TikTokProto.ProtoMap subEmote : chat.getRepeatedMessages(CHAT_EMOTE_FIELD)) {
            int place = subEmote.getInt(1);
            String cacheKey = imageUrl(subEmote.getMessage(2));
            if (cacheKey == null) {
                cacheKey = imageUrl(subEmote);
            }
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
            emotes.add(toEmote(entry.cacheKey, emoteCode, range[0], range[1]));
        }
        return emotes.isEmpty() ? List.of() : List.copyOf(emotes);
    }

    private static MessageEmote toEmote(String cacheKey, String code, int start, int end) {
        return new MessageEmote(PROVIDER, cacheKey, code, start, end);
    }

    /** EmoteModel: картинка в поле 2. Голый id без URL не возвращаем. */
    private static String imageUrl(TikTokProto.ProtoMap emote) {
        if (emote == null) {
            return null;
        }
        String fromImage = extractImageUrl(emote.getMessage(2));
        if (fromImage != null) {
            return fromImage;
        }
        return firstHttpUrl(emote.getString(1));
    }

    private static String extractImageUrl(TikTokProto.ProtoMap image) {
        if (image == null) {
            return null;
        }
        List<String> urls = new ArrayList<>();
        addHttp(urls, image.getString(1));
        for (String url : image.getRepeatedStrings(1)) {
            addHttp(urls, url);
        }
        addHttp(urls, image.getString(2));
        String best = preferRaster(urls);
        if (best != null) {
            return best;
        }
        return uriFallback(image.getString(2));
    }

    private static void addHttp(List<String> urls, String value) {
        String http = firstHttpUrl(value);
        if (http != null && !urls.contains(http)) {
            urls.add(http);
        }
    }

    /** PNG и JPEG декодируются, WebP — нет. Подписанный WebP не переписываем: подпись сломается. */
    private static String preferRaster(List<String> urls) {
        String png = null;
        String jpeg = null;
        String webp = null;
        String other = null;
        for (String url : urls) {
            String path = pathOf(url);
            if (path.endsWith(".png")) {
                png = url;
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                jpeg = url;
            } else if (path.endsWith(".webp") || path.contains(".webp")) {
                webp = url;
            } else if (other == null) {
                other = url;
            }
        }
        if (png != null) {
            return png;
        }
        if (jpeg != null) {
            return jpeg;
        }
        if (webp != null) {
            String rewritten = unsignedPng(webp);
            return rewritten != null ? rewritten : null;
        }
        return other;
    }

    private static String uriFallback(String uri) {
        if (uri == null || uri.isBlank() || uri.contains(" ") || uri.contains("://")) {
            return null;
        }
        String path = uri.toLowerCase(java.util.Locale.ROOT);
        if (!(path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
            || path.endsWith(".webp") || path.endsWith(".gif"))) {
            return null;
        }
        return preferRaster(List.of("https://p16-webcast.tiktokcdn.com/obj/" + uri));
    }

    private static String unsignedPng(String url) {
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("x-signature") || lower.contains("x-expires")) {
            return null;
        }
        int queryAt = url.indexOf('?');
        String query = queryAt >= 0 ? url.substring(queryAt) : "";
        String bare = queryAt >= 0 ? url.substring(0, queryAt) : url;
        String path = bare.toLowerCase(java.util.Locale.ROOT);
        if (path.endsWith(".webp")) {
            return bare.substring(0, bare.length() - 5) + ".png" + query;
        }
        int tilde = bare.lastIndexOf('~');
        if (tilde > 0 && (path.endsWith(".image") || path.contains("webp"))) {
            return bare.substring(0, tilde) + "~tplv-obj.png" + query;
        }
        return null;
    }

    private static String pathOf(String url) {
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        int queryAt = lower.indexOf('?');
        return queryAt >= 0 ? lower.substring(0, queryAt) : lower;
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

    record Stickers(String text, List<MessageEmote> emotes) {}
}
