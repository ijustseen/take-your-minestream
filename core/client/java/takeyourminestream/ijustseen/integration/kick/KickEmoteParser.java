package takeyourminestream.ijustseen.integration.kick;

import takeyourminestream.ijustseen.messages.MessageEmote;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Парсинг `[emote:id:name]` из Kick {@code ChatMessageEvent.content}. */
final class KickEmoteParser {
    static final String PROVIDER = "kick";
    private static final Pattern EMOTE_TOKEN = Pattern.compile("\\[emote:(\\d+):([^\\]]+)]");
    private static final String CDN = "https://files.kick.com/emotes/%s/fullsize";

    private KickEmoteParser() {}

    static List<MessageEmote> parse(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        Matcher matcher = EMOTE_TOKEN.matcher(content);
        List<MessageEmote> emotes = new ArrayList<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            String token = matcher.group();
            emotes.add(new MessageEmote(
                PROVIDER,
                String.format(CDN, id),
                token,
                matcher.start(),
                matcher.end() - 1
            ));
        }
        return emotes.isEmpty() ? List.of() : List.copyOf(emotes);
    }
}
