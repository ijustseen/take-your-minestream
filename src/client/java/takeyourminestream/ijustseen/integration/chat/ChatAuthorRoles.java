package takeyourminestream.ijustseen.integration.chat;

import takeyourminestream.ijustseen.config.ChatRoleFilter;

public record ChatAuthorRoles(
    boolean subscriber,
    boolean vip,
    boolean moderator,
    boolean broadcaster
) {
    public static final ChatAuthorRoles NONE = new ChatAuthorRoles(false, false, false, false);

    /**
     * Унифицированный разбор бейджей/ролей с разных платформ.
     * subscriber = саб/мember, broadcaster = владелец канала/стрим.
     */
    public static ChatAuthorRoles fromBadgeHints(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        boolean subscriber = containsAny(lower,
            "subscriber", "founder", "member", "premium", "sub_gifter", "sponsor", "superfan");
        boolean vip = containsAny(lower, "vip", "super_fan", "top_gifter", "gifter");
        boolean moderator = containsAny(lower, "moderator", "mod_badge", "\"mod\"");
        boolean broadcaster = containsAny(lower, "broadcaster", "owner", "streamer", "anchor", "host", "og");
        return new ChatAuthorRoles(subscriber, vip, moderator, broadcaster);
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public boolean passes(ChatRoleFilter filter) {
        if (filter == null || filter == ChatRoleFilter.ALL) {
            return true;
        }
        return switch (filter) {
            case SUBSCRIBERS -> subscriber;
            case VIP -> vip;
            case MODS -> moderator || broadcaster;
            case SUB_OR_VIP -> subscriber || vip;
            case SUB_OR_MOD -> subscriber || moderator || broadcaster;
            default -> true;
        };
    }
}
