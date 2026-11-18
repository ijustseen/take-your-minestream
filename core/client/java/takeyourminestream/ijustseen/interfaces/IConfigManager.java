package takeyourminestream.ijustseen.interfaces;

/**
 * Интерфейс для управления конфигурацией мода
 */
public interface IConfigManager {
    /**
     * Загружает конфигурацию из файла
     */
    void loadConfig();
    
    /**
     * Сохраняет конфигурацию в файл
     */
    void saveConfig();
    
    /**
     * Получает значение конфигурации по ключу
     * @param key ключ конфигурации
     * @return значение конфигурации
     */
    Object getConfigValue(String key);
    
    /**
     * Устанавливает значение конфигурации
     * @param key ключ конфигурации
     * @param value новое значение
     */
    void setConfigValue(String key, Object value);
} 