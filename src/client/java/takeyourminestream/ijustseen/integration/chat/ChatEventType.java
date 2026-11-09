package takeyourminestream.ijustseen.integration.chat;

/** Вид входящего сообщения: обычный чат или событие платформы. */
public enum ChatEventType {
    CHAT,
    GIFT,
    FOLLOW,
    SUBSCRIBE,
    RAID,
    BITS;

    public boolean isEvent() {
        return this != CHAT;
    }

    public String badgeKey() {
        return switch (this) {
            case FOLLOW -> "event_follow";
            case SUBSCRIBE -> "event_sub";
            case GIFT -> "event_gift";
            case RAID -> "event_raid";
            case BITS -> "event_bits";
            default -> null;
        };
    }

    /** Цвет рамки уведомления, чтобы оно не сливалось с обычным чатом. */
    public int accentRgb() {
        return switch (this) {
            case FOLLOW -> 0x3DDC97;
            case SUBSCRIBE -> 0xC77DFF;
            case GIFT -> 0xFFB347;
            case RAID -> 0x5B8CFF;
            case BITS -> 0x9B59B6;
            default -> 0xFFFFFF;
        };
    }

    public String labelKey() {
        return switch (this) {
            case FOLLOW -> "takeyourstreamchat.event.kind.follow";
            case SUBSCRIBE -> "takeyourstreamchat.event.kind.subscribe";
            case GIFT -> "takeyourstreamchat.event.kind.gift";
            case RAID -> "takeyourstreamchat.event.kind.raid";
            case BITS -> "takeyourstreamchat.event.kind.bits";
            default -> "";
        };
    }

    /** Короткая плашка события: {@code §a§l[FOLLOW]§r}. */
    public String decoratedLabel() {
        if (!isEvent()) {
            return "";
        }
        String color = switch (this) {
            case FOLLOW -> "§a";
            case SUBSCRIBE -> "§d";
            case GIFT -> "§6";
            case RAID -> "§9";
            case BITS -> "§5";
            default -> "§f";
        };
        String tag = switch (this) {
            case FOLLOW -> "FOLLOW";
            case SUBSCRIBE -> "SUB";
            case GIFT -> "GIFT";
            case RAID -> "RAID";
            case BITS -> "DONATION";
            default -> "";
        };
        return color + "§l[" + tag + "]§r";
    }
}
