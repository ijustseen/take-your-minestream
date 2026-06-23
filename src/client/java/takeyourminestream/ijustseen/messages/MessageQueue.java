package takeyourminestream.ijustseen.messages;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Очередь сообщений перед показом в мире/HUD.
 * Размер ограничен: при переполнении отбрасываются сообщения с самым поздним показом.
 * Поддерживает отложенный показ ({@code readyAtTick}) — для пачек YouTube по timestamp.
 */
public class MessageQueue {
    public static class QueuedMessage {
        public final String text;
        public final Integer authorColorRgb;
        public final java.util.List<MessageEmote> emotes;
        /** Тик, начиная с которого сообщение можно показать. */
        public final int readyAtTick;
        final long sequence;

        public QueuedMessage(String text, Integer authorColorRgb, java.util.List<MessageEmote> emotes, int readyAtTick, long sequence) {
            this.text = text;
            this.authorColorRgb = authorColorRgb;
            this.emotes = emotes == null ? java.util.Collections.emptyList() : java.util.List.copyOf(emotes);
            this.readyAtTick = readyAtTick;
            this.sequence = sequence;
        }
    }

    private static final Comparator<QueuedMessage> ORDER = Comparator
        .comparingInt((QueuedMessage m) -> m.readyAtTick)
        .thenComparingLong(m -> m.sequence);

    /** Макс. ожидающих сообщений; лишние (с самым поздним показом) отбрасываются. */
    private static final int MAX_QUEUE_SIZE = 10;

    private final PriorityBlockingQueue<QueuedMessage> messageQueue = new PriorityBlockingQueue<>(11, ORDER);
    private final AtomicLong sequence = new AtomicLong();

    public void enqueueMessage(String message, Integer authorColorRgb, java.util.List<MessageEmote> emotes, int readyAtTick) {
        offerWithCap(new QueuedMessage(message, authorColorRgb, emotes, readyAtTick, sequence.getAndIncrement()));
    }

    private void offerWithCap(QueuedMessage message) {
        messageQueue.offer(message);
        trimToMaxSize();
    }

    private void trimToMaxSize() {
        while (messageQueue.size() > MAX_QUEUE_SIZE) {
            QueuedMessage toDrop = messageQueue.stream()
                .max(Comparator.comparingInt((QueuedMessage m) -> m.readyAtTick).thenComparingLong(m -> m.sequence))
                .orElse(null);
            if (toDrop == null) {
                break;
            }
            messageQueue.remove(toDrop);
        }
    }

    public QueuedMessage pollReady(int currentTick) {
        QueuedMessage head = messageQueue.peek();
        if (head != null && head.readyAtTick <= currentTick) {
            return messageQueue.poll();
        }
        return null;
    }

    public void clear() {
        messageQueue.clear();
    }

    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }

    public int pendingCount() {
        return messageQueue.size();
    }
}
