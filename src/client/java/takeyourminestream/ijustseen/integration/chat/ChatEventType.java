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
}
