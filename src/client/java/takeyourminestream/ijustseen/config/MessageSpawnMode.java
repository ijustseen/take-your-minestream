package takeyourminestream.ijustseen.config;

/**
 * Режимы отображения сообщений
 */
public enum MessageSpawnMode {
    /**
     * Сообщения появляются вокруг игрока в 3D пространстве
     */
    AROUND_PLAYER("around_player"),
    
    /**
     * Сообщения появляются только перед игроком в 3D пространстве
     */
    FRONT_OF_PLAYER("front_of_player"),
    
    /**
     * Сообщения отображаются как HUD виджет в правом верхнем углу экрана
     */
    HUD_WIDGET("hud_widget");
    
    private final String key;
    
    MessageSpawnMode(String key) {
        this.key = key;
    }
    
    public String getKey() {
        return key;
    }
    
    /**
     * Получить режим по ключу
     */
    public static MessageSpawnMode fromKey(String key) {
        for (MessageSpawnMode mode : values()) {
            if (mode.key.equals(key)) {
                return mode;
            }
        }
        return AROUND_PLAYER; // значение по умолчанию
    }
    
    /**
     * Получить следующий режим для переключения
     */
    public MessageSpawnMode next() {
        MessageSpawnMode[] values = values();
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }
}