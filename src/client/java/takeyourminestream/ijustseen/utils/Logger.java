package takeyourminestream.ijustseen.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.logging.Level;

/**
 * Утилитарный класс для логирования и отправки сообщений игроку
 */
public class Logger {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("TakeYourMineStream");
    
    /**
     * Логирует информационное сообщение
     * @param message сообщение для логирования
     */
    public static void info(String message) {
        LOGGER.info(message);
    }
    
    /**
     * Логирует предупреждение
     * @param message сообщение для логирования
     */
    public static void warning(String message) {
        LOGGER.warning(message);
    }
    
    /**
     * Логирует ошибку
     * @param message сообщение для логирования
     */
    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }
    
    /**
     * Логирует ошибку с исключением
     * @param message сообщение для логирования
     * @param throwable исключение
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * Отправляет сообщение игроку
     * @param message сообщение для отправки
     */
    public static void sendToPlayer(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(message), false);
        }
    }
    
    /**
     * Отправляет информационное сообщение игроку
     * @param message сообщение для отправки
     */
    public static void sendInfoToPlayer(String message) {
        sendToPlayer("§a" + message);
    }
    
    /**
     * Отправляет предупреждение игроку
     * @param message сообщение для отправки
     */
    public static void sendWarningToPlayer(String message) {
        sendToPlayer("§e" + message);
    }
    
    /**
     * Отправляет ошибку игроку
     * @param message сообщение для отправки
     */
    public static void sendErrorToPlayer(String message) {
        sendToPlayer("§c" + message);
    }
} 