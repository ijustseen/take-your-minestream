package takeyourminestream.modid;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import takeyourminestream.modid.messages.MessageSpawner;
import takeyourminestream.modid.utils.Logger;

/**
 * Обработчик событий мира для управления паузой системы сообщений
 */
public class WorldEventHandler {
    
    /**
     * Регистрирует обработчики событий входа/выхода из мира
     * @param messageSpawner система спавна сообщений
     */
    public static void register(MessageSpawner messageSpawner) {
        // При выходе из мира - ставим на паузу
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (messageSpawner != null) {
                messageSpawner.pause();
                Logger.info("Система сообщений поставлена на паузу (выход из игры)");
            }
        });
    }
    
    /**
     * Обработчик входа в мир - снимает с паузы
     * Вызывается вручную при входе в мир
     * @param messageSpawner система спавна сообщений
     */
    public static void onWorldJoin(MessageSpawner messageSpawner) {
        if (messageSpawner != null && messageSpawner.isPaused()) {
            messageSpawner.resume();
            Logger.info("Система сообщений снята с паузы (вход в мир)");
        }
    }
    
    /**
     * Обработчик выхода из мира - ставит на паузу
     * Вызывается вручную при выходе из мира
     * @param messageSpawner система спавна сообщений
     */
    public static void onWorldLeave(MessageSpawner messageSpawner) {
        if (messageSpawner != null && !messageSpawner.isPaused()) {
            messageSpawner.pause();
            Logger.info("Система сообщений поставлена на паузу (выход из мира)");
        }
    }
}
