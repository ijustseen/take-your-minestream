package takeyourminestream.ijustseen.interfaces;

import takeyourminestream.ijustseen.messages.MessageSpawner;

/**
 * Интерфейс для управления Twitch-соединением
 */
public interface ITwitchManager {
    /**
     * Подключается к Twitch-каналу
     * @param spawner спавнер сообщений
     */
    void connect(MessageSpawner spawner);
    
    /**
     * Отключается от Twitch-канала
     */
    void disconnect();
    
    /**
     * Проверяет, подключен ли к Twitch
     * @return true если подключен
     */
    boolean isConnected();
    
    /**
     * Обрабатывает изменение имени канала
     * @param newChannelName новое имя канала
     */
    void onChannelNameChanged(String newChannelName);
} 