package takeyourminestream.ijustseen.filtering;

import net.minecraft.client.util.TextCollector;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import xeed.mc.streamotes.Compat;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Optional;
import java.util.regex.Matcher;

public class StreamoteRenderer {

    public static StringVisitable emotifyIfPossible (StringVisitable text) {

        TextCollector textCollector = new TextCollector();
        text.visit((style, part) -> {
            maybeStyled(textCollector, part, style);
            return Optional.empty();
        }, Style.EMPTY);
        return textCollector.getCombined();
    }

    private static void maybeStyled(TextCollector textCollector, String string, Style style) {
        Matcher matcher = Streamotes.EMOTE_PATTERN.matcher(string);
        int lastEnd = 0;

        while(matcher.find()) {
            String name = matcher.group();
            Emoticon emoticon = EmoticonRegistry.fromName(name);

            if (emoticon != null) {
                //Streamotes.log("Emote found: " + emoticon.getName());
                int start = matcher.start();
                if (start > lastEnd && !(emoticon.getChatRenderWidth() == 0.0F && string.substring(lastEnd, start).isBlank())) {
                    textCollector.add(StringVisitable.styled(string.substring(lastEnd, start), style));
                }

                textCollector.add(StringVisitable.styled(emoticon.getName(), Compat.makeEmoteStyle(emoticon).withParent(style)));
                lastEnd = matcher.end();
            }
        }

        if (lastEnd < string.length()) {
            textCollector.add(StringVisitable.styled(string.substring(lastEnd), style));
        }
    }
}
