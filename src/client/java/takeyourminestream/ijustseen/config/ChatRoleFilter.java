package takeyourminestream.ijustseen.config;

import java.util.HashSet;
import java.util.Set;

/**
 * Фильтр ролей Twitch-чата. Определяет, какие сообщения показывать по IRC-бейджам.
 */
public enum ChatRoleFilter {
    ALL("all"),
    SUBSCRIBERS("subscribers"),
    VIP("vip"),
    MODS("mods"),
    SUB_OR_VIP("sub_or_vip"),
    SUB_OR_MOD("sub_or_mod");

    private final String key;

    ChatRoleFilter(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static ChatRoleFilter fromKey(String key) {
        if (key == null) {
            return ALL;
        }
        for (ChatRoleFilter filter : values()) {
            if (filter.key.equals(key)) {
                return filter;
            }
        }
        return ALL;
    }

    public ChatRoleFilter next() {
        ChatRoleFilter[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    /**
     * Проверяет, проходит ли сообщение фильтр ролей.
     *
     * @param badgesTag значение IRC-тега badges, например "subscriber/12,vip/1"
     */
    public boolean passes(String badgesTag) {
        if (this == ALL) {
            return true;
        }

        Set<String> badges = parseBadgeTypes(badgesTag);
        boolean isSub = badges.contains("subscriber") || badges.contains("founder") || badges.contains("premium");
        boolean isVip = badges.contains("vip");
        boolean isMod = badges.contains("moderator") || badges.contains("broadcaster");

        return switch (this) {
            case SUBSCRIBERS -> isSub;
            case VIP -> isVip;
            case MODS -> isMod;
            case SUB_OR_VIP -> isSub || isVip;
            case SUB_OR_MOD -> isSub || isMod;
            default -> true;
        };
    }

    static Set<String> parseBadgeTypes(String badgesTag) {
        Set<String> types = new HashSet<>();
        if (badgesTag == null || badgesTag.isEmpty()) {
            return types;
        }
        for (String part : badgesTag.split(",")) {
            if (part.isEmpty()) {
                continue;
            }
            int slash = part.indexOf('/');
            types.add(slash > 0 ? part.substring(0, slash) : part);
        }
        return types;
    }
}
