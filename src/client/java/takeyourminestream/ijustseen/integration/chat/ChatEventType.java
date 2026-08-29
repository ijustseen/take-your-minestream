package takeyourminestream.ijustseen.integration.chat;

/** Вид входящего сообщения: обычный чат или событие платформы. */
public enum ChatEventType {
    CHAT,
    GIFT,
    FOLLOW;

    public boolean isEvent() {
        return this != CHAT;
    }
}
