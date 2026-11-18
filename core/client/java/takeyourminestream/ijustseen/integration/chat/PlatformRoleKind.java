package takeyourminestream.ijustseen.integration.chat;

import java.util.List;

/**
 * Роли, которые источник реально отдаёт в чате и которые можно фильтровать.
 * {@link #ALL} — не роль, а режим «показывать всех».
 */
public enum PlatformRoleKind {
    ALL("all"),
    FOLLOWERS("followers"),
    SUBSCRIBERS("subs"),
    VIP("vip"),
    MODS("mods");

    private final String key;

    PlatformRoleKind(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean isAll() {
        return this == ALL;
    }

    /** Порядок на странице источника: сначала «все», затем доступные роли. */
    public static List<PlatformRoleKind> filtersFor(ChatPlatform platform) {
        return switch (platform) {
            case TWITCH -> List.of(ALL, SUBSCRIBERS, VIP, MODS);
            case YOUTUBE -> List.of(ALL, SUBSCRIBERS, MODS);
            case KICK -> List.of(ALL, SUBSCRIBERS, VIP, MODS);
            case TIKTOK -> List.of(ALL, FOLLOWERS, SUBSCRIBERS, MODS);
        };
    }

    public String labelKey(ChatPlatform platform) {
        if (this == ALL) {
            return "takeyourstreamchat.config.role_filter_all";
        }
        return "takeyourstreamchat.config.role_filter." + platform.getIconKey() + "." + key;
    }
}
