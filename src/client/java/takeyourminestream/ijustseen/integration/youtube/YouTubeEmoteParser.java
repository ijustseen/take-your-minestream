package takeyourminestream.ijustseen.integration.youtube;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import takeyourminestream.ijustseen.messages.MessageEmote;

import java.util.ArrayList;
import java.util.List;

/**
 * Собирает текст YouTube live chat и инлайн-эмодзи из {@code message.runs}
 * (именованные вроде {@code face-purple-crying} и кастомные эмодзи канала).
 */
final class YouTubeEmoteParser {
    static final String PROVIDER = "youtube";
    private static final String PLACEHOLDER = "\uFFFC";

    private YouTubeEmoteParser() {}

    record Parsed(String text, List<MessageEmote> emotes) {
        static Parsed empty() {
            return new Parsed("", List.of());
        }
    }

    static Parsed parse(JsonObject renderer) {
        if (renderer == null || !renderer.has("message") || !renderer.get("message").isJsonObject()) {
            return Parsed.empty();
        }
        JsonObject messageObj = renderer.getAsJsonObject("message");
        if (messageObj.has("simpleText")) {
            String simple = messageObj.get("simpleText").getAsString();
            return new Parsed(simple != null ? simple : "", List.of());
        }
        if (!messageObj.has("runs") || !messageObj.get("runs").isJsonArray()) {
            return Parsed.empty();
        }

        StringBuilder text = new StringBuilder();
        List<MessageEmote> emotes = new ArrayList<>();
        for (JsonElement runEl : messageObj.getAsJsonArray("runs")) {
            if (!runEl.isJsonObject()) {
                continue;
            }
            appendRun(runEl.getAsJsonObject(), text, emotes);
        }
        return new Parsed(text.toString(), emotes.isEmpty() ? List.of() : List.copyOf(emotes));
    }

    private static void appendRun(JsonObject run, StringBuilder text, List<MessageEmote> emotes) {
        if (run.has("emoji") && run.get("emoji").isJsonObject()) {
            JsonObject emoji = run.getAsJsonObject("emoji");
            String imageUrl = bestImageUrl(emoji);
            String placeholder = placeholderFor(run, emoji);
            if (imageUrl != null) {
                int start = text.length();
                text.append(placeholder);
                emotes.add(new MessageEmote(
                    PROVIDER,
                    imageUrl,
                    placeholder,
                    start,
                    text.length() - 1
                ));
                return;
            }
            text.append(placeholder);
            return;
        }
        if (run.has("text")) {
            text.append(run.get("text").getAsString());
        }
    }

    private static String placeholderFor(JsonObject run, JsonObject emoji) {
        if (run.has("text")) {
            String text = run.get("text").getAsString();
            if (text != null && !text.isEmpty()) {
                return text;
            }
        }
        if (emoji.has("shortcuts") && emoji.get("shortcuts").isJsonArray()) {
            JsonArray shortcuts = emoji.getAsJsonArray("shortcuts");
            if (!shortcuts.isEmpty() && shortcuts.get(0).isJsonPrimitive()) {
                String shortcut = shortcuts.get(0).getAsString();
                if (shortcut != null && !shortcut.isBlank()) {
                    return shortcut;
                }
            }
        }
        if (emoji.has("emojiId") && emoji.get("emojiId").isJsonPrimitive()) {
            String id = emoji.get("emojiId").getAsString();
            if (id != null && !id.isBlank() && id.indexOf('/') < 0) {
                return ":" + id + ":";
            }
        }
        return PLACEHOLDER;
    }

    private static String bestImageUrl(JsonObject emoji) {
        if (!emoji.has("image") || !emoji.get("image").isJsonObject()) {
            return null;
        }
        JsonObject image = emoji.getAsJsonObject("image");
        if (!image.has("thumbnails") || !image.get("thumbnails").isJsonArray()) {
            return null;
        }
        String best = null;
        int bestWidth = -1;
        for (JsonElement thumbEl : image.getAsJsonArray("thumbnails")) {
            if (!thumbEl.isJsonObject()) {
                continue;
            }
            JsonObject thumb = thumbEl.getAsJsonObject();
            if (!thumb.has("url") || !thumb.get("url").isJsonPrimitive()) {
                continue;
            }
            String url = normalizeUrl(thumb.get("url").getAsString());
            if (url == null || looksLikeSvg(url)) {
                continue;
            }
            int width = thumb.has("width") && thumb.get("width").isJsonPrimitive()
                ? thumb.get("width").getAsInt()
                : 0;
            if (width >= bestWidth) {
                bestWidth = width;
                best = url;
            }
        }
        return best;
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return null;
    }

    private static boolean looksLikeSvg(String url) {
        String lower = url.toLowerCase();
        int query = lower.indexOf('?');
        String path = query >= 0 ? lower.substring(0, query) : lower;
        return path.endsWith(".svg");
    }
}
