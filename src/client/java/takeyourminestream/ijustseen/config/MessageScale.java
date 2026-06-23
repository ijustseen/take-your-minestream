package takeyourminestream.ijustseen.config;

/**
 * Размеры/масштабы сообщений
 */
public enum MessageScale {
    /**
     * Очень маленькие сообщения
     */
    TINY("tiny", 0.35f),
    
    /**
     * Маленькие сообщения
     */
    SMALL("small", 0.55f),
    
    /**
     * Обычные сообщения (базовый масштаб = 1.0)
     */
    NORMAL("normal", 0.75f),
    
    /**
     * Большие сообщения
     */
    LARGE("large", 1.0f),
    
    /**
     * Огромные сообщения (бывший «Large» до уменьшения шкалы)
     */
    HUGE("huge", 1.25f);
    
    private final String key;
    private final float scale;
    
    MessageScale(String key, float scale) {
        this.key = key;
        this.scale = scale;
    }
    
    public String getKey() {
        return key;
    }
    
    public float getScale() {
        return scale;
    }
    
    /**
     * Получить размер по ключу
     */
    public static MessageScale fromKey(String key) {
        for (MessageScale scale : values()) {
            if (scale.key.equals(key)) {
                return scale;
            }
        }
        return NORMAL; // значение по умолчанию
    }
    
    /**
     * Получить следующий размер для переключения
     */
    public MessageScale next() {
        MessageScale[] values = values();
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }
    
    /**
     * Получить предыдущий размер для переключения
     */
    public MessageScale previous() {
        MessageScale[] values = values();
        int prevIndex = (this.ordinal() - 1 + values.length) % values.length;
        return values[prevIndex];
    }
}