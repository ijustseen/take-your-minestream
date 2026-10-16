package takeyourminestream.ijustseen.config;

/** Как задаётся Twitch-канал: ник или ключ трансляции из Creator Dashboard. */
public enum TwitchConnectMethod {
    CHANNEL,
    STREAM_KEY;

    public TwitchConnectMethod toggle() {
        return this == CHANNEL ? STREAM_KEY : CHANNEL;
    }

    public boolean usesStreamKey() {
        return this == STREAM_KEY;
    }

    public String getKey() {
        return this == STREAM_KEY ? "stream_key" : "channel";
    }
}
