package takeyourminestream.ijustseen.messages;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Управляет очередью сообщений для отображения
 */
public class MessageQueue {
    public static class QueuedMessage {
        public final String text;
        public final Integer authorColorRgb; // может быть null
        public final java.util.List<MessageEmote> emotes;
        public QueuedMessage(String text, Integer authorColorRgb, java.util.List<MessageEmote> emotes) {
            this.text = text;
            this.authorColorRgb = authorColorRgb;
            this.emotes = emotes == null ? java.util.Collections.emptyList() : java.util.List.copyOf(emotes);
        }
    }

    private final Queue<QueuedMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private long lastMessageSpawnTime = 0;
    private static final int MIN_TICKS_BETWEEN_MESSAGES = 0; // Минимум тиков между спавном сообщений
    
    /**
     * Добавляет сообщение в очередь
     * @param message текст сообщения
     */
    public void enqueueMessage(String message) {
        messageQueue.offer(new QueuedMessage(message, null, java.util.Collections.emptyList()));
    }

    public void enqueueMessage(String message, Integer authorColorRgb) {
        messageQueue.offer(new QueuedMessage(message, authorColorRgb, java.util.Collections.emptyList()));
    }

    public void enqueueMessage(String message, Integer authorColorRgb, java.util.List<MessageEmote> emotes) {
        messageQueue.offer(new QueuedMessage(message, authorColorRgb, emotes));
    }
    
    /**
     * Проверяет, можно ли спавнить новое сообщение
     * @param currentTick текущий тик
     * @return true, если можно спавнить сообщение
     */
    public boolean canSpawnMessage(int currentTick) {
        return !messageQueue.isEmpty() && 
               (currentTick - lastMessageSpawnTime >= MIN_TICKS_BETWEEN_MESSAGES);
    }
    
    /**
     * Извлекает следующее сообщение из очереди
     * @param currentTick текущий тик
     * @return текст сообщения или null, если очередь пуста
     */
    public QueuedMessage dequeueMessage(int currentTick) {
        if (canSpawnMessage(currentTick)) {
            QueuedMessage message = messageQueue.poll();
            if (message != null) {
                lastMessageSpawnTime = currentTick;
            }
            return message;
        }
        return null;
    }
    
    /**
     * Очищает очередь сообщений
     */
    public void clear() {
        messageQueue.clear();
        lastMessageSpawnTime = 0;
    }
    
    /**
     * Проверяет, пуста ли очередь
     * @return true, если очередь пуста
     */
    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }
} 