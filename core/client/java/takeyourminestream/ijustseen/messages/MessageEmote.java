package takeyourminestream.ijustseen.messages;

import java.util.List;

public class MessageEmote {
    public static final String GIF_PROVIDER = "gif";
    public static final int DEFAULT_ICON_SIZE = 12;
    public static final int GIF_ICON_SIZE = 32;
    public static final int MIN_GIF_SIZE = 24;
    public static final int GIF_LINE_GAP = 3;

    private final String provider;
    private final String emoteId;
    private final String emoteCode;
    private final int startIndex;
    private final int endIndex;

    public MessageEmote(String provider, String emoteId, String emoteCode, int startIndex, int endIndex) {
        this.provider = provider;
        this.emoteId = emoteId;
        this.emoteCode = emoteCode;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public String getProvider() {
        return provider;
    }

    public String getEmoteId() {
        return emoteId;
    }

    public String getEmoteCode() {
        return emoteCode;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public boolean isGif() {
        return GIF_PROVIDER.equals(provider);
    }

    public int iconSize() {
        return isGif() ? GIF_ICON_SIZE : DEFAULT_ICON_SIZE;
    }

    public int displaySize(int gifSize) {
        return isGif() && gifSize > 0 ? gifSize : iconSize();
    }

    /** Сторона квадрата гифки: ширина строки с ником и иконками. */
    public static int fitGifSize(int usernameRowWidth) {
        return Math.max(MIN_GIF_SIZE, usernameRowWidth);
    }

    public static int maxIconSize(List<MessageEmote> emotes) {
        int max = DEFAULT_ICON_SIZE;
        if (emotes == null) {
            return max;
        }
        for (MessageEmote emote : emotes) {
            max = Math.max(max, emote.iconSize());
        }
        return max;
    }
}