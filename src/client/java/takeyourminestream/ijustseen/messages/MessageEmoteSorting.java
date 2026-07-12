package takeyourminestream.ijustseen.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Валидация и сортировка inline-эмотов для рендеринга строки. */
public final class MessageEmoteSorting {
    private MessageEmoteSorting() {}

    public static List<MessageEmote> sortedValid(String text, List<MessageEmote> emotes) {
        if (emotes == null || emotes.isEmpty()) {
            return Collections.emptyList();
        }

        List<MessageEmote> sorted = new ArrayList<>(emotes);
        sorted.sort(Comparator.comparingInt(MessageEmote::getStartIndex));

        List<MessageEmote> valid = new ArrayList<>();
        int nextAllowedStart = 0;
        for (MessageEmote emote : sorted) {
            if (emote.getStartIndex() < 0
                || emote.getEndIndex() < emote.getStartIndex()
                || emote.getEndIndex() >= text.length()) {
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
