package takeyourminestream.modid.messages;

public class MessageEmote {
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
}