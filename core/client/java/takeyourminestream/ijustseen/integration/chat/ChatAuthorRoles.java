package takeyourminestream.ijustseen.integration.chat;

import takeyourminestream.ijustseen.config.ChatRoleFilter;

public record ChatAuthorRoles(
    boolean follower,
    boolean subscriber,
    boolean vip,
    boolean moderator,
    boolean broadcaster
) {
    public static final ChatAuthorRoles NONE = new ChatAuthorRoles(false, false, false, false, false);

    /**
     * Сообщение проходит фильтр: «показывать всех» — без проверки ролей;
     * иначе достаточно любой включённой роли (ИЛИ).
     */
    public boolean passesSelectedRoles(
        boolean showAll,
        boolean followers,
        boolean subscribers,
        boolean vip,
        boolean mods
    ) {
        if (showAll) {
            return true;
        }
        if (followers && follower) {
            return true;
        }
        if (subscribers && subscriber) {
            return true;
        }
        if (vip && this.vip) {
            return true;
        }
        return mods && (moderator || broadcaster);
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
            case VIP_OR_MOD -> vip || moderator || broadcaster;
            case SUB_OR_VIP_OR_MOD -> subscriber || vip || moderator || broadcaster;
            default -> true;
        };
    }

    /** Twitch IRC {@code badges}: {@code subscriber/12,vip/1,moderator/1}. Фолловеров в теге нет. */
    public static ChatAuthorRoles fromTwitchBadges(String badgesTag) {
        if (badgesTag == null || badgesTag.isBlank()) {
            return NONE;
        }
        boolean subscriber = false;
        boolean vip = false;
        boolean moderator = false;
        boolean broadcaster = false;
        for (String part : badgesTag.split(",")) {
            int slash = part.indexOf('/');
            String name = (slash >= 0 ? part.substring(0, slash) : part).trim().toLowerCase();
            switch (name) {
                case "subscriber", "founder" -> subscriber = true;
                case "vip" -> vip = true;
                case "moderator" -> moderator = true;
                case "broadcaster" -> broadcaster = true;
                default -> { }
            }
        }
        return new ChatAuthorRoles(false, subscriber, vip, moderator, broadcaster);
    }
}
