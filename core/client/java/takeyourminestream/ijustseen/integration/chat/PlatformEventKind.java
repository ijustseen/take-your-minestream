package takeyourminestream.ijustseen.integration.chat;

import java.util.List;

/** События эфира, которые можно включать отдельно на странице платформы. */
public enum PlatformEventKind {
    FOLLOW("follow"),
    SUBSCRIBE("subscribe"),
    GIFT("gift"),
    RAID("raid"),
    BITS("bits");

    private final String key;

    PlatformEventKind(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static PlatformEventKind of(ChatEventType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case FOLLOW -> FOLLOW;
            case SUBSCRIBE -> SUBSCRIBE;
            case GIFT -> GIFT;
            case RAID -> RAID;
            case BITS -> BITS;
            default -> null;
        };
    }

    public static List<PlatformEventKind> filtersFor(ChatPlatform platform) {
        return switch (platform) {
            case TWITCH -> List.of(SUBSCRIBE, GIFT, RAID, BITS);
            case YOUTUBE -> List.of(SUBSCRIBE, GIFT, BITS);
            case KICK -> List.of(FOLLOW, SUBSCRIBE, GIFT, RAID);
            case TIKTOK -> List.of(FOLLOW, SUBSCRIBE, GIFT);
        };
    }

    public String labelKey(ChatPlatform platform) {
        return "takeyourstreamchat.config.event." + platform.getIconKey() + "." + key;
    }
}
