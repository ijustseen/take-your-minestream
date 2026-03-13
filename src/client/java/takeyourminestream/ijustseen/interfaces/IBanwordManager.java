package takeyourminestream.ijustseen.interfaces;

/**
 * Интерфейс для управления банвордами
 */
public interface IBanwordManager {
    /**
     * Загружает банворды из файла
     */
    void loadBanwords();
    
    /**
     * Проверяет, содержит ли сообщение банворды
     * @param message сообщение для проверки
     * @return true если содержит банворды
     */
    boolean containsBanwords(String message);
    
    /**
     * Фильтрует банворды из сообщения
     * @param message исходное сообщение
     * @return отфильтрованное сообщение
     */
    String filterBanwords(String message);
    
    /**
     * Добавляет новый банворд
     * @param banword банворд для добавления
     */
    void addBanword(String banword);
    
    /**
     * Удаляет банворд
     * @param banword банворд для удаления
     */
    void removeBanword(String banword);

    /**
     * Возвращает неизменяемый набор текущих банвордов
     */
    java.util.Set<String> getBanwords();
} 